package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import de.uni.hamburg.postprocessor.sphinxbased.node.HMMNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.WordNode;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.WordSequence;

/**
 * Represents the final end of utterance word
 */
class LexTreeEndWordState extends LexTreeWordSearchState implements WordSearchState {

    /**
     * Constructs a LexTreeWordSearchState
     *
     * @param wordNode       the word node
     * @param lastNode       the previous word node
     * @param wordSequence   the sequence of words triphone context
     * @param logProbability the probability of this word occurring
     */
    LexTreeEndWordState(final LexTreeLinguistAndre lexTreeLinguistAndre, WordNode wordNode, HMMNode lastNode,
        WordSequence wordSequence, float smearTerm, float smearProb, float logProbability) {
        super(lexTreeLinguistAndre, wordNode, lastNode, wordSequence, smearTerm, smearProb, logProbability);
    }

    @Override
    public int getOrder() {
        return 2;
    }

}
