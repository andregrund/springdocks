package de.uni.hamburg.postprocessor.sphinxbased.node;

import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Utilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages a single entry point.
 */
public class EntryPoint {

    private HMMTree hmmTree;

    private final Unit baseUnit;

    private final Node baseNode;      // second units and beyond start here

    private final Map<Unit, Node> unitToEntryPointMap;

    private List<Pronunciation> singleUnitWords;

    private int nodeCount;

    private Set<Unit> rcSet;

    private float totalProbability;

    /**
     * Creates an entry point for the given unit
     *
     * @param baseUnit the EntryPoint is created for this unit
     */
    public EntryPoint(final HMMTree hmmTree, Unit baseUnit) {
        this.hmmTree = hmmTree;
        this.baseUnit = baseUnit;
        this.baseNode = new Node(LogMath.LOG_ZERO);
        this.unitToEntryPointMap = new HashMap<Unit, Node>();
        this.singleUnitWords = new ArrayList<Pronunciation>();
        this.totalProbability = LogMath.LOG_ZERO;
    }

    /**
     * Given a left context get a node that represents a single set of entry points into this unit
     *
     * @param leftContext the left context of interest
     * @return the node representing the entry point
     */
    Node getEntryPointsFromLeftContext(Unit leftContext) {
        return unitToEntryPointMap.get(leftContext);
    }

    /**
     * Accumulates the probability for this entry point
     *
     * @param probability a new  probability
     */
    void addProbability(float probability) {
        if (probability > totalProbability) {
            totalProbability = probability;
        }
    }

    /**
     * Returns the probability for all words reachable from this node
     *
     * @return the log probability
     */
    float getProbability() {
        return totalProbability;
    }

    /**
     * Once we have built the full entry point we can eliminate some fields
     */
    void freeze() {
        for (Node node : unitToEntryPointMap.values()) {
            node.freeze();
        }
        singleUnitWords = null;
        rcSet = null;
    }

    /**
     * Gets the base node for this entry point
     *
     * @return the base node
     */
    Node getNode() {
        return baseNode;
    }

    /**
     * Adds a one-unit word to this entry point. Such single unit words need to be dealt with specially.
     *
     * @param p the pronunciation of the single unit word
     */
    void addSingleUnitWord(Pronunciation p) {
        singleUnitWords.add(p);
    }

    /**
     * Gets the set of possible right contexts that we can transition to from this entry point
     *
     * @return the set of possible transition points.
     */
    private Collection<Unit> getEntryPointRC() {
        if (rcSet == null) {
            rcSet = new HashSet<Unit>();
            for (Node node : baseNode.getSuccessorMap().values()) {
                UnitNode unitNode = (UnitNode) node;
                rcSet.add(unitNode.getBaseUnit());
            }
        }
        return rcSet;
    }

    /**
     * A version of createEntryPointMap that compresses common hmms across all entry points.
     */
    void createEntryPointMap() {
        HashMap<HMM, Node> map = new HashMap<HMM, Node>();
        HashMap<HMM, HMMNode> singleUnitMap = new HashMap<HMM, HMMNode>();

        for (Unit lc : hmmTree.exitPoints) {
            Node epNode = new Node(LogMath.LOG_ZERO);
            for (Unit rc : getEntryPointRC()) {
                HMM hmm = hmmTree.hmmPool.getHMM(baseUnit, lc, rc, HMMPosition.BEGIN);
                Node addedNode;

                if ((addedNode = map.get(hmm)) == null) {
                    addedNode = epNode.addSuccessor(hmm, getProbability());
                    map.put(hmm, addedNode);
                } else {
                    epNode.putSuccessor(hmm, addedNode);
                }

                nodeCount++;
                connectEntryPointNode(addedNode, rc);
            }
            connectSingleUnitWords(lc, epNode, singleUnitMap);
            unitToEntryPointMap.put(lc, epNode);
        }
    }

    /**
     * Connects the single unit words associated with this entry point.   The singleUnitWords list contains all
     * single unit pronunciations that have as their sole unit, the unit associated with this entry point. Entry
     * points for these words are added to the epNode for all possible left (exit) and right (entry) contexts.
     *
     * @param lc     the left context
     * @param epNode the entry point node
     */
    private void connectSingleUnitWords(Unit lc, Node epNode, HashMap<HMM, HMMNode> map) {
        if (!singleUnitWords.isEmpty()) {

            for (Unit rc : hmmTree.entryPoints) {
                HMM hmm = hmmTree.hmmPool.getHMM(baseUnit, lc, rc, HMMPosition.SINGLE);

                HMMNode tailNode;
                if ((tailNode = map.get(hmm)) == null) {
                    tailNode = (HMMNode) epNode.addSuccessor(hmm, getProbability());
                    map.put(hmm, tailNode);
                } else {
                    epNode.putSuccessor(hmm, tailNode);
                }
                WordNode wordNode;
                tailNode.addRC(rc);
                nodeCount++;

                for (Pronunciation p : singleUnitWords) {
                    if (p.getWord() == hmmTree.dictionary.getSentenceStartWord()) {
                        hmmTree.initialNode = new InitialWordNode(p, tailNode);
                    } else {
                        float prob = hmmTree.getWordUnigramProbability(p.getWord());
                        wordNode = tailNode.addSuccessor(p, prob, hmmTree.wordNodeMap);
                        if (p.getWord() == hmmTree.dictionary.getSentenceEndWord()) {
                            hmmTree.sentenceEndWordNode = wordNode;
                        }
                    }
                    nodeCount++;
                }
            }
        }
    }

    /**
     * Connect the entry points that match the given rc to the given epNode
     *
     * @param epNode add matching successors here
     * @param rc     the next unit
     */
    private void connectEntryPointNode(Node epNode, Unit rc) {
        for (Node node : baseNode.getSuccessors()) {
            UnitNode successor = (UnitNode) node;
            if (successor.getBaseUnit() == rc) {
                epNode.addSuccessor(successor);
            }
        }
    }

    /**
     * Dumps the entry point
     */
    void dump() {
        System.out.println("EntryPoint " + baseUnit + " RC Followers: " + getEntryPointRC().size());
        int count = 0;
        Collection<Unit> rcs = getEntryPointRC();
        System.out.print("    ");
        for (Unit rc : rcs) {
            System.out.print(Utilities.pad(rc.getName(), 4));
            if (count++ >= 12) {
                count = 0;
                System.out.println();
                System.out.print("    ");
            }
        }
        System.out.println();
    }
}
