package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import de.uni.hamburg.postprocessor.sphinxbased.node.EndNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.HMMNode;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.Unit;

/**
 * Represents a unit in the search space
 */
class LexTreeEndUnitState extends LexTreeState implements UnitSearchState {

    private LexTreeLinguistAndre lexTreeLinguistAndre;

    private float logLanguageProbability;

    private float logInsertionProbability;

    /**
     * Constructs a LexTreeUnitState
     *
     * @param wordSequence the history of words
     */
    LexTreeEndUnitState(final LexTreeLinguistAndre lexTreeLinguistAndre, EndNode endNode, WordSequence wordSequence,
        float smearTerm, float smearProb, float languageProbability, float insertionProbability) {

        super(lexTreeLinguistAndre, endNode, wordSequence, smearTerm, smearProb);
        this.lexTreeLinguistAndre = lexTreeLinguistAndre;
        logLanguageProbability = languageProbability;
        logInsertionProbability = insertionProbability;
        // System.out.println("LTEUS " + logLanguageProbability + " " +
        // logInsertionProbability);
    }

    /**
     * Returns the base unit associated with this state
     *
     * @return the base unit
     */
    public Unit getUnit() {
        return getEndNode().getBaseUnit();
    }

    /**
     * Generate a hashcode for an object
     *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        return super.hashCode() * 17 + 423;
    }

    /**
     * Gets the acoustic probability of entering this state
     *
     * @return the log probability
     */
    @Override
    public float getInsertionProbability() {
        return logInsertionProbability;
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
     * Determines if the given object is equal to this object
     *
     * @param o the object to test
     * @return <code>true</code> if the object is equal to this
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof LexTreeEndUnitState && super.equals(o);
    }

    /**
     * Returns the unit node for this state
     *
     * @return the unit node
     */
    private EndNode getEndNode() {
        return (EndNode) getNode();
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
            HMMNode[] nodes = lexTreeLinguistAndre.getHMMNodes(getEndNode());
            arcs = new SearchStateArc[nodes.length];

            if (lexTreeLinguistAndre.generateUnitStates) {
                for (int i = 0; i < nodes.length; i++) {
                    arcs[i] = new LexTreeUnitState(lexTreeLinguistAndre, nodes[i], getWordHistory(), getSmearTerm(),
                        getSmearProb(), lexTreeLinguistAndre.logOne, lexTreeLinguistAndre.logOne, this.getNode());
                }
            } else {
                for (int i = 0; i < nodes.length; i++) {
                    HMM hmm = nodes[i].getHMM();
                    arcs[i] = new LexTreeHMMState(lexTreeLinguistAndre, nodes[i], getWordHistory(), getSmearTerm(),
                        getSmearProb(), hmm.getInitialState(), lexTreeLinguistAndre.logOne, lexTreeLinguistAndre.logOne,
                        this.getNode());
                }
            }
            putCachedArcs(arcs);
        }
        return arcs;
    }

    @Override
    public String toString() {
        return super.toString() + " EndUnit";
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
