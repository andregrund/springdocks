package de.uni.hamburg.postprocessor.sphinxbased.node;

import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;

public abstract class UnitNode extends Node {

    public final static int SIMPLE_UNIT = 1;

    public final static int WORD_BEGINNING_UNIT = 2;

    public final static int SILENCE_UNIT = 3;

    public final static int FILLER_UNIT = 4;

    private int type;

    /**
     * Creates the UnitNode
     *
     * @param probablilty the probability for the node
     */
    UnitNode(float probablilty) {
        super(probablilty);
    }

    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    public abstract Unit getBaseUnit();

    abstract Object getKey();

    abstract HMMPosition getPosition();

    /**
     * Gets the unit type (one of SIMPLE_UNIT, WORD_BEGINNING_UNIT, SIMPLE_UNIT or FILLER_UNIT
     *
     * @return the unit type
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the unit type
     *
     * @param type the unit type
     */
    void setType(int type) {
        this.type = type;
    }

}
