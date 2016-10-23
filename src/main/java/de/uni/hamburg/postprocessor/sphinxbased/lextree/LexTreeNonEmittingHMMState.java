package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import de.uni.hamburg.postprocessor.sphinxbased.lextree.LexTreeHMMState;
import de.uni.hamburg.postprocessor.sphinxbased.lextree.LexTreeLinguistAndre;
import de.uni.hamburg.postprocessor.sphinxbased.node.Node;
import de.uni.hamburg.postprocessor.sphinxbased.node.UnitNode;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMMState;

/**
 * Represents a non emitting hmm state
 */
class LexTreeNonEmittingHMMState extends LexTreeHMMState {

    private LexTreeLinguistAndre lexTreeLinguistAndre;

    /**
     * Constructs a NonEmittingLexTreeHMMState
     *
     * @param hmmState     the hmm state associated with this unit
     * @param wordSequence the word history
     * @param probability  the probability of the transition occurring
     */
    LexTreeNonEmittingHMMState(final LexTreeLinguistAndre lexTreeLinguistAndre, UnitNode unitNode,
        WordSequence wordSequence, float smearTerm, float smearProb, HMMState hmmState, float probability,
        Node parentNode) {
        super(lexTreeLinguistAndre, unitNode, wordSequence, smearTerm, smearProb, hmmState, lexTreeLinguistAndre.logOne,
            probability, parentNode);
        this.lexTreeLinguistAndre = lexTreeLinguistAndre;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
