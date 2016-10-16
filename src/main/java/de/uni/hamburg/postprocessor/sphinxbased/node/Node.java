package de.uni.hamburg.postprocessor.sphinxbased.node;

import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node in the HMM Tree
 */

// For large vocabularies we may create millions of these objects,
// therefore they are extremely space sensitive. So we want to make
// these objects as small as possible.  The requirements for these
// objects when building the tree of nodes are very different from once
// we have built it. When building, we need to easily add successor
// nodes and quickly identify duplicate children nodes. After the tree
// is built we just need to quickly identify successors.  We want the
// flexibility of a map to manage successors at startup, but we don't
// want the space penalty (at least 5 32 bit fields per map), instead
// we'd like an array.  To support this dual mode, we manage the
// successors in an Object which can either be a Map or a List
// depending upon whether the node has been frozen or not.

public class Node {

    private static int nodeCount;

    private static int successorCount;

    /**
     * This can be either Map during tree construction or Array after
     * tree freeze. Conversion to array helps to save memory.
     */
    private Object successors;

    private float logUnigramProbability;

    /**
     * Creates a node
     *
     * @param probability the unigram probability for the node
     */
    Node(float probability) {
        logUnigramProbability = probability;
        nodeCount++;
        //        if ((nodeCount % 10000) == 0) {
        //             System.out.println("NC " + nodeCount);
        //        }
    }

    /**
     * Returns the unigram probability
     *
     * @return the unigram probability
     */
    public float getUnigramProbability() {
        return logUnigramProbability;
    }

    /**
     * Sets the unigram probability
     *
     * @param probability the unigram probability
     */
    public void setUnigramProbability(float probability) {
        logUnigramProbability = probability;
    }

    /**
     * Given an object get the set of successors for this object
     *
     * @param key the object key
     * @return the node containing the successors
     */
    private Node getSuccessor(Object key) {
        Map<Object, Node> successors = getSuccessorMap();
        return successors.get(key);
    }

    /**
     * Add the child to the set of successors
     *
     * @param key   the object key
     * @param child the child to add
     */
    void putSuccessor(Object key, Node child) {
        Map<Object, Node> successors = getSuccessorMap();
        successors.put(key, child);
    }

    /**
     * Gets the successor map for this node
     *
     * @return the successor map
     */
    @SuppressWarnings({"unchecked"})
    public Map<Object, Node> getSuccessorMap() {
        if (successors == null) {
            successors = new HashMap<Object, Node>(4);
        }

        assert successors instanceof Map;
        return (Map<Object, Node>) successors;
    }

    /**
     * Freeze the node. Convert the successor map into an array list
     */
    void freeze() {
        if (successors instanceof Map<?, ?>) {
            Map<Object, Node> map = getSuccessorMap();
            successors = map.values().toArray(new Node[map.size()]);
            for (Node node : map.values()) {
                node.freeze();
            }
            successorCount += map.size();
        }
    }

    static void dumpNodeInfo() {
        System.out
            .println("Nodes: " + nodeCount + " successors " + successorCount + " avg " + (successorCount / nodeCount));
    }

    /**
     * Adds a child node holding an hmm to the successor.  If a node similar to the child has already been added, we use
     * the previously added node, otherwise we add this. Also, we record the base unit of the child in the set of right
     * context
     *
     * @param hmm the hmm to add
     * @return the node that holds the hmm (new or old)
     */
    Node addSuccessor(HMM hmm, float probability) {
        Node child = null;
        Node matchingChild = getSuccessor(hmm);
        if (matchingChild == null) {
            child = new HMMNode(hmm, probability);
            putSuccessor(hmm, child);
        } else {
            if (matchingChild.getUnigramProbability() < probability) {
                matchingChild.setUnigramProbability(probability);
            }
            child = matchingChild;
        }
        return child;
    }

    /**
     * Adds a child node holding a pronunciation to the successor. If a node similar to the child has already been
     * added, we use the previously added node, otherwise we add this. Also, we record the base unit of the child in the
     * set of right context
     *
     * @param pronunciation the pronunciation to add
     * @param wordNodeMap
     * @return the node that holds the pronunciation (new or old)
     */
    WordNode addSuccessor(Pronunciation pronunciation, float probability, Map<Pronunciation, WordNode> wordNodeMap) {
        WordNode child = null;
        WordNode matchingChild = (WordNode) getSuccessor(pronunciation);
        if (matchingChild == null) {
            child = wordNodeMap.get(pronunciation);
            if (child == null) {
                child = new WordNode(pronunciation, probability);
                wordNodeMap.put(pronunciation, child);
            }
            putSuccessor(pronunciation, child);
        } else {
            if (matchingChild.getUnigramProbability() < probability) {
                matchingChild.setUnigramProbability(probability);
            }
            child = matchingChild;
        }
        return child;
    }

    void addSuccessor(WordNode wordNode) {
        putSuccessor(wordNode, wordNode);
    }

    /**
     * Adds an EndNode to the set of successors for this node If a node similar to the child has already been added, we
     * use the previously added node, otherwise we add this.
     *
     * @param child       the endNode to add
     * @param probability probability for this transition
     * @return the node that holds the endNode (new or old)
     */
    EndNode addSuccessor(EndNode child, float probability) {
        Unit baseUnit = child.getBaseUnit();
        EndNode matchingChild = (EndNode) getSuccessor(baseUnit);
        if (matchingChild == null) {
            putSuccessor(baseUnit, child);
        } else {
            if (matchingChild.getUnigramProbability() < probability) {
                matchingChild.setUnigramProbability(probability);
            }
            child = matchingChild;
        }
        return child;
    }

    /**
     * Adds a child node to the successor.  If a node similar to the child has already been added, we use the previously
     * added node, otherwise we add this. Also, we record the base unit of the child in the set of right context
     *
     * @param child the child to add
     * @return the node (may be different than child if there was already a node attached holding the hmm held by
     * child)
     */
    UnitNode addSuccessor(UnitNode child) {
        UnitNode matchingChild = (UnitNode) getSuccessor(child.getKey());
        if (matchingChild == null) {
            putSuccessor(child.getKey(), child);
        } else {
            child = matchingChild;
        }

        return child;
    }

    /**
     * Returns the successors for this node
     *
     * @return the set of successor nodes
     */
    public Node[] getSuccessors() {
        if (successors instanceof Map<?, ?>) {
            freeze();
        }
        return (Node[]) successors;
    }

    /**
     * Returns the string representation for this object
     *
     * @return the string representation of the object
     */
    @Override
    public String toString() {
        return "Node ";
    }
}
