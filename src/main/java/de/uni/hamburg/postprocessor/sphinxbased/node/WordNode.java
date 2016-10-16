package de.uni.hamburg.postprocessor.sphinxbased.node;

import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;

/**
 * A node representing a word in the HMM tree
 */
public class WordNode extends Node {

    private final Pronunciation pronunciation;

    private final boolean isFinal;

    /**
     * Creates a word node
     *
     * @param pronunciation the pronunciation to wrap in this node
     * @param probability   the word unigram probability
     */
    WordNode(Pronunciation pronunciation, float probability) {
        super(probability);
        this.pronunciation = pronunciation;
        this.isFinal = pronunciation.getWord().isSentenceEndWord();
    }

    /**
     * Gets the word associated with this node
     *
     * @return the word
     */
    public Word getWord() {
        return pronunciation.getWord();
    }

    /**
     * Gets the pronunciation associated with this node
     *
     * @return the pronunciation
     */
    public Pronunciation getPronunciation() {
        return pronunciation;
    }

    /**
     * Gets the last unit for this word
     *
     * @return the last unit
     */
    public Unit getLastUnit() {
        Unit[] units = pronunciation.getUnits();
        return units[units.length - 1];
    }

    /**
     * Returns the successors for this node
     *
     * @return the set of successor nodes
     */
    @Override
    public Node[] getSuccessors() {
        throw new Error("Not supported");
    }

    /**
     * Returns a string representation for this object
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "WordNode " + pronunciation + " p " + getUnigramProbability();
    }

    public boolean isFinal() {
        return isFinal;
    }
}
