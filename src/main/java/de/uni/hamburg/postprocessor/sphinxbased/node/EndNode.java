package de.uni.hamburg.postprocessor.sphinxbased.node;

import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;

public class EndNode extends UnitNode {

    private final Unit baseUnit;

    private final Unit leftContext;

    private final Integer key;

    /**
     * Creates the node, wrapping the given hmm
     *
     * @param baseUnit    the base unit for this node
     * @param lc          the left context
     * @param probablilty the probability for the transition to this node
     */
    EndNode(Unit baseUnit, Unit lc, float probablilty) {
        super(probablilty);
        this.baseUnit = baseUnit;
        this.leftContext = lc;
        key = baseUnit.getBaseID() * 121 + leftContext.getBaseID();
    }

    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    @Override
    public Unit getBaseUnit() {
        return baseUnit;
    }

    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    Unit getLeftContext() {
        return leftContext;
    }

    @Override
    Integer getKey() {
        return key;
    }

    @Override
    HMMPosition getPosition() {
        return HMMPosition.END;
    }

    /**
     * Returns a string representation for this object
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "EndNode base:" + baseUnit + " lc " + leftContext + ' ' + key;
    }

    /**
     * Freeze this node. Convert the set into an array to reduce memory overhead
     */
    @Override
    void freeze() {
        super.freeze();
    }
}
