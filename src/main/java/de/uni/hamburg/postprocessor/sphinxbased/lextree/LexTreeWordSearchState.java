package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import de.uni.hamburg.postprocessor.sphinxbased.node.HMMNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.Node;
import de.uni.hamburg.postprocessor.sphinxbased.node.WordNode;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a word state in the search space
 */
class LexTreeWordSearchState extends LexTreeState implements WordSearchState {

    private LexTreeLinguistAndre lexTreeLinguistAndre;

    private HMMNode lastNode;

    private float logLanguageProbability;

    /**
     * Constructs a LexTreeWordSearchState
     *
     * @param wordNode            the word node
     * @param wordSequence        the sequence of words triphone context
     * @param languageProbability the probability of this word
     */
    LexTreeWordSearchState(final LexTreeLinguistAndre lexTreeLinguistAndre, final WordNode wordNode,
        final HMMNode lastNode, final WordSequence wordSequence, final float smearTerm, final float smearProb,
        final float languageProbability) {

        super(lexTreeLinguistAndre, wordNode, wordSequence, smearTerm, smearProb);
        this.lexTreeLinguistAndre = lexTreeLinguistAndre;
        this.lastNode = lastNode;
        this.logLanguageProbability = languageProbability;
    }

    /**
     * Gets the word pronunciation for this state
     *
     * @return the pronunciation for this word
     */
    public Pronunciation getPronunciation() {
        return ((WordNode) getNode()).getPronunciation();
    }

    /**
     * Determines if this is a final state
     *
     * @return <code>true</code> if this is an final state.
     */
    @Override
    public boolean isFinal() {
        return ((WordNode) getNode()).isFinal();
    }

    /**
     * Generate a hashcode for an object
     *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        return super.hashCode() * 41 + lastNode.hashCode();
    }

    /**
     * Gets the unique signature for this state. The signature building code is slow and should only be used for
     * non-time-critical tasks such as plotting states.
     *
     * @return the signature
     */
    @Override
    public String getSignature() {
        return super.getSignature() + "-ln-" + lastNode.hashCode();
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
        } else if (o instanceof LexTreeWordSearchState) {
            LexTreeWordSearchState other = (LexTreeWordSearchState) o;
            return lastNode == other.lastNode && super.equals(o);
        } else {
            return false;
        }
    }

    /**
     * Gets the language probability of entering this state
     *
     * @return the log probability
     */
    @Override
    public float getLanguageProbability() {
        return logLanguageProbability;
    }

    /**
     * Returns the list of successors to this state
     *
     * @return a list of SearchState objects
     */
    @Override
    public SearchStateArc[] getSuccessors() {
        SearchStateArc[] arcs = getCachedArcs();
        if (arcs == null) {
            arcs = LexTreeLinguistAndre.EMPTY_ARC;
            WordNode wordNode = (WordNode) getNode();

            if (wordNode.getWord() != lexTreeLinguistAndre.sentenceEndWord) {
                int index = 0;
                List<Node> nodeList = new ArrayList<>();
                Unit[] rightContext = lastNode.getRightContexts();
                Unit left = wordNode.getLastUnit();

                for (final Unit unit : rightContext) {
                    Node[] nodes = lexTreeLinguistAndre.hmmTree.getEntryPoint(left, unit);
                    Collections.addAll(nodeList, nodes);
                }

                // add a link to every possible entry point as well
                // as link to the </s> node
                arcs = new SearchStateArc[nodeList.size() + 1];
                for (Node node : nodeList) {
                    arcs[index++] = createUnitStateArc((HMMNode) node, this);
                }

                // now add the link to the end of sentence arc:

                arcs[index++] = createWordStateArc(lexTreeLinguistAndre.hmmTree.getSentenceEndWordNode(), lastNode,
                    this);
            }
            putCachedArcs(arcs);
        }
        return arcs;
    }

    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * Returns true if this LexTreeWordSearchState indicates the start of a word. Returns false if this LexTreeWordSearchState
     * indicates the end of a word.
     *
     * @return true if this LexTreeWordSearchState indicates the start of a word, false if this LexTreeWordSearchState indicates
     * the end of a word
     */
    public boolean isWordStart() {
        return false;
    }
}
