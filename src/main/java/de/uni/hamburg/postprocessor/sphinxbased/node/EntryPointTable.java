package de.uni.hamburg.postprocessor.sphinxbased.node;

import edu.cmu.sphinx.linguist.acoustic.Unit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The EntryPoint table is used to manage the set of entry points into the lex tree.
 */
public class EntryPointTable {

    private final Map<Unit, EntryPoint> entryPoints;

    /**
     * Create the entry point table give the set of all possible entry point units
     *
     * @param entryPointCollection the set of possible entry points
     */
    public EntryPointTable(final HMMTree hmmTree, Collection<Unit> entryPointCollection) {
        entryPoints = new HashMap<>();
        for (Unit unit : entryPointCollection) {
            entryPoints.put(unit, new EntryPoint(hmmTree, unit));
        }
    }

    /**
     * Given a CI unit, return the EntryPoint object that manages the entry point for the unit
     *
     * @param baseUnit the unit of interest (A ci unit)
     * @return the object that manages the entry point for the unit
     */
    EntryPoint getEntryPoint(Unit baseUnit) {
        return entryPoints.get(baseUnit);
    }

    /**
     * Creates the entry point maps for all entry points.
     */
    void createEntryPointMaps() {
        for (EntryPoint ep : entryPoints.values()) {
            ep.createEntryPointMap();
        }
    }

    /**
     * Freezes the entry point table
     */
    void freeze() {
        for (EntryPoint ep : entryPoints.values()) {
            ep.freeze();
        }
    }

    /**
     * Dumps the entry point table
     */
    void dump() {
        for (EntryPoint ep : entryPoints.values()) {
            ep.dump();
        }
    }
}
