package de.uni.hamburg.postprocessor.sphinxbased.node;

import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.util.LogMath;

/**
 * A class that represents the initial word in the search space. It is treated specially because we need to keep track
 * of the context as well. The context is embodied in the parent node
 */
public class InitialWordNode extends WordNode {

    private final HMMNode parent;

    /**
     * Creates an InitialWordNode
     *
     * @param pronunciation the pronunciation
     * @param parent        the parent node
     */
    InitialWordNode(Pronunciation pronunciation, HMMNode parent) {
        super(pronunciation, LogMath.LOG_ONE);
        this.parent = parent;
    }

    /**
     * Gets the parent for this word node
     *
     * @return the parent
     */
    public HMMNode getParent() {
        return parent;
    }

}
