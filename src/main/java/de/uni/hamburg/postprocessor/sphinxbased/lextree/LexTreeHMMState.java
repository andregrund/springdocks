package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import de.uni.hamburg.data.PhoneData;
import de.uni.hamburg.postprocessor.sphinxbased.node.HMMNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.Node;
import de.uni.hamburg.postprocessor.sphinxbased.node.UnitNode;
import edu.cmu.sphinx.decoder.scorer.ScoreProvider;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.HMMStateArc;
import edu.cmu.sphinx.linguist.acoustic.Unit;

/**
 * Represents a HMM state in the search space
 */
class LexTreeHMMState extends LexTreeState implements HMMSearchState, ScoreProvider {

    private LexTreeLinguistAndre lexTreeLinguistAndre;

    private final HMMState hmmState;

    private float logLanguageProbability;

    private float logInsertionProbability;

    private final Node parentNode;

    private int hashCode = -1;

    private int numberOfTimesUsed = 0;

    /**
     * Constructs a LexTreeHMMState
     *
     * @param unitNode             the HMM state associated with this unit
     * @param wordSequence         the word history
     * @param languageProbability  the probability of the transition
     * @param insertionProbability the probability of the transition
     */
    LexTreeHMMState(final LexTreeLinguistAndre lexTreeLinguistAndre, UnitNode unitNode, WordSequence wordSequence,
        float smearTerm, float smearProb, HMMState hmmState, float languageProbability, float insertionProbability,
        Node parentNode) {
        super(lexTreeLinguistAndre, unitNode, wordSequence, smearTerm, smearProb);
        this.lexTreeLinguistAndre = lexTreeLinguistAndre;
        this.hmmState = hmmState;
        this.parentNode = parentNode;
        this.logLanguageProbability = languageProbability;
        this.logInsertionProbability = insertionProbability;
    }

    /**
     * Gets the ID for this state
     *
     * @return the ID
     */
    @Override
    public String getSignature() {
        return super.getSignature() + "-HMM-" + hmmState.getState();
    }

    /**
     * returns the HMM state associated with this state
     *
     * @return the HMM state
     */
    public HMMState getHMMState() {
        return hmmState;
    }

    /**
     * Generate a hashcode for an object
     *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = super.hashCode() * 29 + (hmmState.getState() + 1);
            if (parentNode != null) {
                hashCode *= 377;
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
        } else if (o instanceof LexTreeHMMState) {
            LexTreeHMMState other = (LexTreeHMMState) o;
            return hmmState == other.hmmState && parentNode == other.parentNode && super.equals(o);
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
     * Gets the language probability of entering this state
     *
     * @return the log probability
     */
    @Override
    public float getInsertionProbability() {
        return logInsertionProbability;
    }

    /**
     * Retrieves the set of successors for this state
     *
     * @return the list of successor states
     */
    @Override
    public SearchStateArc[] getSuccessors() {
        SearchStateArc[] nextStates = getCachedArcs();
        if (nextStates == null) {

            // if this is an exit state, we are transitioning to a
            // new unit or to a word end.

            if (hmmState.isExitState()) {
                if (parentNode == null) {
                    nextStates = super.getSuccessors();
                } else {
                    nextStates = super.getSuccessors(parentNode);
                }
            } else {
                // The current hmm state is not an exit state, so we
                // just go through the next set of successors

                HMMStateArc[] arcs = hmmState.getSuccessors();
                nextStates = new SearchStateArc[arcs.length];
                for (int i = 0; i < arcs.length; i++) {
                    HMMStateArc arc = arcs[i];
                    if (arc.getHMMState().isEmitting()) {
                        // if its a self loop and the prob. matches
                        // reuse the state
                        if (arc.getHMMState() == hmmState && logInsertionProbability == arc.getLogProbability()) {
                            nextStates[i] = this;
                        } else {
                            nextStates[i] = new LexTreeHMMState(lexTreeLinguistAndre, (UnitNode) getNode(),
                                getWordHistory(), getSmearTerm(), getSmearProb(), arc.getHMMState(),
                                lexTreeLinguistAndre.logOne, arc.getLogProbability(), parentNode);
                        }
                    } else {
                        nextStates[i] = new LexTreeNonEmittingHMMState(lexTreeLinguistAndre, (UnitNode) getNode(),
                            getWordHistory(), getSmearTerm(), getSmearProb(), arc.getHMMState(),
                            arc.getLogProbability(), parentNode);
                    }
                }
            }
            putCachedArcs(nextStates);
        }
        return nextStates;
    }

    /**
     * Determines if this is an emitting state
     */
    @Override
    public boolean isEmitting() {
        return hmmState.isEmitting();
    }

    @Override
    public String toString() {
        return super.toString() + " hmm:" + hmmState;
    }

    @Override
    public int getOrder() {
        return 5;
    }

    public float getScore(Data data) {
        numberOfTimesUsed++;
        final HMMNode node = (HMMNode) getNode();
        final Unit baseUnit = node.getBaseUnit();
        final String baseUnitName = baseUnit.getName();

        //TODO: find the name
        return ((PhoneData) data).getConfusionScore(baseUnitName, numberOfTimesUsed);
    }

    public float[] getComponentScore(Data feature) {
        return hmmState.calculateComponentScore(feature);
    }

}
