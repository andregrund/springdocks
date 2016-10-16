package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import de.uni.hamburg.postprocessor.sphinxbased.lextree.old.LexTreeHMMState;
import de.uni.hamburg.postprocessor.sphinxbased.node.EndNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.HMMNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.Node;
import de.uni.hamburg.postprocessor.sphinxbased.node.WordNode;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.dictionary.Word;

/**
 * The LexTreeLinguist returns language states to the search manager. This class forms the base implementation for
 * all language states returned. This LexTreeState keeps track of the probability of entering this state (a
 * language+insertion probability) as well as the unit history. The unit history consists of the LexTree nodes that
 * correspond to the left, center and right contexts.
 * This is an abstract class, subclasses must implement the getSuccessorss method.
 */
abstract class LexTreeState implements SearchState, SearchStateArc {

    private LexTreeLinguistAndre lexTreeLinguistAndre;

    private final Node node;

    private final WordSequence wordSequence;

    private final float currentSmearTerm;

    private final float currentSmearProb;

    /**
     * Creates a LexTreeState.
     *
     * @param node         the node associated with this state
     * @param wordSequence the history of words up until this point
     */
    LexTreeState(final LexTreeLinguistAndre lexTreeLinguistAndre, final Node node, final WordSequence wordSequence,
        final float smearTerm, final float smearProb) {
        this.lexTreeLinguistAndre = lexTreeLinguistAndre;
        this.node = node;
        this.wordSequence = wordSequence;
        currentSmearTerm = smearTerm;
        currentSmearProb = smearProb;
    }

    /**
     * Gets the unique signature for this state. The signature building code is slow and should only be used for
     * non-time-critical tasks such as plotting states.
     *
     * @return the signature
     */
    public String getSignature() {
        return "lts-" + node.hashCode() + "-ws-" + wordSequence;
    }

    public float getSmearTerm() {
        return currentSmearTerm;
    }

    public float getSmearProb() {
        return currentSmearProb;
    }

    /**
     * Generate a hashcode for an object
     *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        int hashCode = wordSequence.hashCode() * 37;
        hashCode += node.hashCode();
        return hashCode;
    }

    /**
     * Determines if the given object is equal to this object
     *
     * @param o the object to test
     * @return <code>true</code> if the object is equal to this
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof LexTreeState) {
            LexTreeState other = (LexTreeState) o;
            if (node != other.node)
                return false;
            return wordSequence.equals(other.wordSequence);
        } else {
            return false;
        }
    }

    /**
     * Gets a successor to this search state
     *
     * @return the successor state
     */
    public SearchState getState() {
        return this;
    }

    /**
     * Gets the composite probability of entering this state
     *
     * @return the log probability
     */
    public float getProbability() {
        return getLanguageProbability() + getInsertionProbability();
    }

    /**
     * Gets the language probability of entering this state
     *
     * @return the log probability
     */
    public float getLanguageProbability() {
        return lexTreeLinguistAndre.logOne;
    }

    /**
     * Gets the insertion probability of entering this state
     *
     * @return the log probability
     */
    public float getInsertionProbability() {
        return lexTreeLinguistAndre.logOne;
    }

    /**
     * Determines if this is an emitting state
     *
     * @return <code>true</code> if this is an emitting state.
     */
    public boolean isEmitting() {
        return false;
    }

    /**
     * Determines if this is a final state
     *
     * @return <code>true</code> if this is an final state.
     */
    public boolean isFinal() {
        return false;
    }

    /**
     * Gets the hmm tree node representing the unit
     *
     * @return the unit lex node
     */
    protected Node getNode() {
        return node;
    }

    /**
     * Returns the word sequence for this state
     *
     * @return the word sequence
     */
    public WordSequence getWordHistory() {
        return wordSequence;
    }

    public Node getLexState() {
        return node;
    }

    /**
     * Returns the list of successors to this state
     *
     * @return a list of SearchState objects
     */
    public SearchStateArc[] getSuccessors() {
        SearchStateArc[] arcs = getCachedArcs();
        if (arcs == null) {
            arcs = getSuccessors(node);
            putCachedArcs(arcs);
        }
        return arcs;
    }

    /**
     * Returns the list of successors to this state
     *
     * @param theNode node to get successors
     * @return a list of SearchState objects
     */
    protected SearchStateArc[] getSuccessors(Node theNode) {
        final Node[] nodeSuccessors = theNode.getSuccessors();
        SearchStateArc[] arcs = new SearchStateArc[nodeSuccessors.length];
        int arcPosition = 0;
        for (Node nextNode : nodeSuccessors) {
            if (nextNode instanceof WordNode) {
                arcs[arcPosition] = createWordStateArc((WordNode) nextNode, (HMMNode) getNode(), this);
            } else if (nextNode instanceof EndNode) {
                arcs[arcPosition] = createEndUnitArc((EndNode) nextNode, this);
            } else {
                // HMMNode
                arcs[arcPosition] = createUnitStateArc((HMMNode) nextNode, this);
            }
            arcPosition++;
        }
        return arcs;
    }

    /**
     * Creates a word search state for the given word node
     *
     * @param wordNode the wordNode
     * @param lastUnit last unit of the word
     * @param previous previous state
     * @return the search state for the wordNode
     */
    SearchStateArc createWordStateArc(WordNode wordNode, HMMNode lastUnit, LexTreeState previous) {
        float languageProbability = lexTreeLinguistAndre.logOne;
        final Word nextWord = wordNode.getWord();
        float smearTerm = previous.getSmearTerm();

        if (nextWord.isFiller() && nextWord != lexTreeLinguistAndre.sentenceEndWord) {
            return new LexTreeWordSearchState(lexTreeLinguistAndre, wordNode, lastUnit, wordSequence, smearTerm,
                lexTreeLinguistAndre.logOne, languageProbability);
        }

        final WordSequence nextWordSequence = wordSequence.addWord(nextWord, lexTreeLinguistAndre.maxDepth);
        float probability =
            lexTreeLinguistAndre.languageModel.getProbability(nextWordSequence) * lexTreeLinguistAndre.languageWeight;
        smearTerm = lexTreeLinguistAndre.getSmearTermFromLanguageModel(nextWordSequence);

        // recalculate languageProbability
        languageProbability = probability - previous.getSmearProb();

        if (nextWord == lexTreeLinguistAndre.sentenceEndWord) {
            return new LexTreeEndWordState(lexTreeLinguistAndre, wordNode, lastUnit,
                nextWordSequence.trim(lexTreeLinguistAndre.maxDepth - 1), smearTerm, lexTreeLinguistAndre.logOne,
                languageProbability);
        }

        return new LexTreeWordSearchState(lexTreeLinguistAndre, wordNode, lastUnit,
            nextWordSequence.trim(lexTreeLinguistAndre.maxDepth - 1), smearTerm, lexTreeLinguistAndre.logOne,
            languageProbability);
    }

    /**
     * Creates a unit search state for the given unit node
     *
     * @param hmmNode the unit node
     * @return the search state
     */
    SearchStateArc createUnitStateArc(HMMNode hmmNode, LexTreeState previous) {
        SearchStateArc arc;
        // System.out.println("CUSA " + hmmNode);
        float insertionProbability = lexTreeLinguistAndre.calculateInsertionProbability(hmmNode);
        float smearProbability = lexTreeLinguistAndre.getUnigramSmear(hmmNode) + previous.getSmearTerm();
        float languageProbability = smearProbability - previous.getSmearProb();

        // if we want a unit state create it, otherwise
        // get the first hmm state of the unit

        if (lexTreeLinguistAndre.generateUnitStates) {
            arc = new LexTreeUnitState(lexTreeLinguistAndre, hmmNode, getWordHistory(), previous.getSmearTerm(),
                smearProbability, languageProbability, insertionProbability);
        } else {
            HMM hmm = hmmNode.getHMM();
            arc = new LexTreeHMMState(lexTreeLinguistAndre, hmmNode, getWordHistory(), previous.getSmearTerm(),
                smearProbability, hmm.getInitialState(), languageProbability, insertionProbability, null);
        }
        return arc;
    }

    /**
     * Creates a unit search state for the given unit node
     *
     * @param endNode  the unit node
     * @param previous the previous state
     * @return the search state
     */
    private SearchStateArc createEndUnitArc(EndNode endNode, LexTreeState previous) {
        float smearProbability = lexTreeLinguistAndre.getUnigramSmear(endNode) + previous.getSmearTerm();
        float languageProbability = smearProbability - previous.getSmearProb();
        float insertionProbability = lexTreeLinguistAndre.calculateInsertionProbability(endNode);
        return new LexTreeEndUnitState(lexTreeLinguistAndre, endNode, getWordHistory(), previous.getSmearTerm(),
            smearProbability, languageProbability, insertionProbability);
    }

    /**
     * Returns the string representation of this object
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "lt-" + node + ' ' + getProbability() + '{' + wordSequence + '}';
    }

    /**
     * Returns a pretty version of the string representation for this object
     *
     * @return a pretty string
     */
    public String toPrettyString() {
        return toString();
    }

    /**
     * Gets the successor arcs for this state from the cache
     *
     * @return the next set of arcs for this state, or null if none can be found or if caching is disabled.
     */
    SearchStateArc[] getCachedArcs() {
        if (lexTreeLinguistAndre.cacheEnabled) {
            SearchStateArc[] arcs = lexTreeLinguistAndre.arcCache.get(this);
            if (arcs != null) {
                lexTreeLinguistAndre.cacheHits++;
            }
            if (++lexTreeLinguistAndre.cacheTrys % 1000000 == 0) {
                System.out.println(
                    "Hits: " + lexTreeLinguistAndre.cacheHits + " of " + lexTreeLinguistAndre.cacheTrys + ' '
                        + ((float) lexTreeLinguistAndre.cacheHits) / lexTreeLinguistAndre.cacheTrys * 100f);
            }
            return arcs;
        } else {
            return null;
        }
    }

    /**
     * Puts the set of arcs into the cache
     *
     * @param arcs the arcs to cache.
     */
    void putCachedArcs(SearchStateArc[] arcs) {
        if (lexTreeLinguistAndre.cacheEnabled) {
            lexTreeLinguistAndre.arcCache.put(this, arcs);
        }
    }

    abstract public int getOrder();
}
