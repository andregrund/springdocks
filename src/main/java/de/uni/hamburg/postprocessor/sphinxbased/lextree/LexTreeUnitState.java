package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import de.uni.hamburg.postprocessor.sphinxbased.lextree.old.LexTreeHMMState;
import de.uni.hamburg.postprocessor.sphinxbased.node.HMMNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.Node;
import de.uni.hamburg.postprocessor.sphinxbased.node.UnitNode;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.Unit;

/**
 * Represents a unit in the search space
 */
class LexTreeUnitState extends LexTreeState implements UnitSearchState {

    private LexTreeLinguistAndre lexTreeLinguistAndre;

    private float logInsertionProbability;

    private float logLanguageProbability;

    private Node parentNode;

    private int hashCode = -1;

    /**
     * Constructs a LexTreeUnitState
     *
     * @param wordSequence the history of words
     */
    LexTreeUnitState(final LexTreeLinguistAndre lexTreeLinguistAndre, UnitNode unitNode, WordSequence wordSequence,
        float smearTerm, float smearProb, float languageProbability, float insertionProbability) {
        this(lexTreeLinguistAndre, unitNode, wordSequence, smearTerm, smearProb, languageProbability,
            insertionProbability, null);
    }

    /**
     * Constructs a LexTreeUnitState
     *
     * @param wordSequence the history of words
     */
    LexTreeUnitState(final LexTreeLinguistAndre lexTreeLinguistAndre, UnitNode unitNode, WordSequence wordSequence,
        float smearTerm, float smearProb, float languageProbability, float insertionProbability, Node parentNode) {
        super(lexTreeLinguistAndre, unitNode, wordSequence, smearTerm, smearProb);
        this.lexTreeLinguistAndre = lexTreeLinguistAndre;
        this.logInsertionProbability = insertionProbability;
        this.logLanguageProbability = languageProbability;
        this.parentNode = parentNode;

    }

    /**
     * Returns the base unit associated with this state
     *
     * @return the base unit
     */
    public Unit getUnit() {

        return getHMMNode().getBaseUnit();
    }

    /**
     * Generate a hashcode for an object
     *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = super.hashCode() * 17 + 421;
            if (parentNode != null) {
                hashCode *= 432;
                hashCode += parentNode.hashCode();
            }
        }
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
        } else if (o instanceof LexTreeUnitState) {
            LexTreeUnitState other = (LexTreeUnitState) o;
            return parentNode == other.parentNode && super.equals(o);
        } else {
            return false;
        }
    }

    /**
     * Returns the unit node for this state
     *
     * @return the unit node
     */
    private HMMNode getHMMNode() {
        return (HMMNode) getNode();
    }

    /**
     * Returns the list of successors to this state
     *
     * @return a list of SearchState objects
     */
    @Override
    public SearchStateArc[] getSuccessors() {
        SearchStateArc[] arcs = new SearchStateArc[1];
        HMM hmm = getHMMNode().getHMM();
        arcs[0] = new LexTreeHMMState(lexTreeLinguistAndre, getHMMNode(), getWordHistory(), getSmearTerm(),
            getSmearProb(), hmm.getInitialState(), lexTreeLinguistAndre.logOne, lexTreeLinguistAndre.logOne,
            parentNode);
        return arcs;
    }

    @Override
    public String toString() {
        return super.toString() + " unit";
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

    @Override
    public int getOrder() {
        return 4;
    }
}
