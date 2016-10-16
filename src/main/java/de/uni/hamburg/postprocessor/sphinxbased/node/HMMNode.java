package de.uni.hamburg.postprocessor.sphinxbased.node;

import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;

import java.util.HashSet;
import java.util.Set;

/**
 * A node that represents an HMM in the hmm tree
 */

public class HMMNode extends UnitNode {

    private final HMM hmm;

    // There can potentially be a large number of nodes (millions),
    // therefore it is important to conserve space as much as
    // possible.  While building the HMMNodes, we keep right contexts
    // in a set to allow easy pruning of duplicates.  Once the tree is
    // entirely built, we no longer need to manage the right contexts
    // as a set, a simple array will do. The freeze method converts
    // the set to the array of units.  This rcSet object holds the set
    // during construction and the array after the freeze.

    private Object rcSet;

    /**
     * Creates the node, wrapping the given hmm
     *
     * @param hmm the hmm to hold
     */
    HMMNode(HMM hmm, float probablilty) {
        super(probablilty);
        this.hmm = hmm;

        Unit base = getBaseUnit();

        int type = SIMPLE_UNIT;
        if (base.isSilence()) {
            type = SILENCE_UNIT;
        } else if (base.isFiller()) {
            type = FILLER_UNIT;
        } else if (hmm.getPosition().isWordBeginning()) {
            type = WORD_BEGINNING_UNIT;
        }
        setType(type);
    }

    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    @Override
    public Unit getBaseUnit() {
        // return hmm.getUnit().getBaseUnit();
        return hmm.getBaseUnit();
    }

    /**
     * Returns the hmm for this node
     *
     * @return the hmm
     */
    public HMM getHMM() {
        return hmm;
    }

    @Override
    HMMPosition getPosition() {
        return hmm.getPosition();
    }

    @Override
    HMM getKey() {
        return getHMM();
    }

    /**
     * Returns a string representation for this object
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "HMMNode " + hmm + " p " + getUnigramProbability();
    }

    /**
     * Adds a right context to the set of possible right contexts for this node. This is typically only needed for hmms
     * at the ends of words.
     *
     * @param rc the right context.
     */
    void addRC(Unit rc) {
        getRightContextSet().add(rc);
    }

    /**
     * Freeze this node. Convert the set into an array to reduce memory overhead
     */
    @Override
    @SuppressWarnings({"unchecked"})
    void freeze() {
        super.freeze();
        if (rcSet instanceof Set) {
            Set<Unit> set = (Set<Unit>) rcSet;
            rcSet = set.toArray(new Unit[set.size()]);
        }
    }

    /**
     * Gets the rc as a set. If we've already been frozen it is an error
     *
     * @return the set of right contexts
     */
    @SuppressWarnings({"unchecked"})
    private Set<Unit> getRightContextSet() {
        if (rcSet == null) {
            rcSet = new HashSet<Unit>();
        }

        assert rcSet instanceof HashSet;
        return (Set<Unit>) rcSet;
    }

    /**
     * returns the set of right contexts for this node
     *
     * @return the set of right contexts
     */
    public Unit[] getRightContexts() {
        if (rcSet instanceof HashSet<?>) {
            freeze();
        }
        return (Unit[]) rcSet;
    }
}
