package de.uni.hamburg.postprocessor.sphinxbased.node;

/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPool;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Utilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Represents the vocabulary as a lex tree with nodes in the tree representing either words (WordNode) or units
 * (HMMNode). HMMNodes may be shared.
 */
public class HMMTree {

    final HMMPool hmmPool;

    InitialWordNode initialNode;

    Dictionary dictionary;

    private LanguageModel lm;

    private final boolean addFillerWords;

    private final boolean addSilenceWord = true;

    final Set<Unit> entryPoints = new HashSet<>();

    Set<Unit> exitPoints = new HashSet<>();

    private Set<Word> allWords;

    private EntryPointTable entryPointTable;

    private boolean debug;

    private final float languageWeight;

    private final Map<Object, HMMNode[]> endNodeMap;

    final Map<Pronunciation, WordNode> wordNodeMap;

    WordNode sentenceEndWordNode;

    private Logger logger;

    /**
     * Creates the HMMTree
     *
     * @param pool           the pool of HMMs and units
     * @param dictionary     the dictionary containing the pronunciations
     * @param lm             the source of the set of words to add to the lex tree
     * @param addFillerWords if <code>false</code> add filler words
     * @param languageWeight the languageWeight
     */
    public HMMTree(HMMPool pool, Dictionary dictionary, LanguageModel lm, boolean addFillerWords, float languageWeight) {
        this.hmmPool = pool;
        this.dictionary = dictionary;
        this.lm = lm;
        this.endNodeMap = new HashMap<Object, HMMNode[]>();
        this.wordNodeMap = new HashMap<Pronunciation, WordNode>();
        this.addFillerWords = addFillerWords;
        this.languageWeight = languageWeight;

        logger = Logger.getLogger(HMMTree.class.getSimpleName());
        compile();
    }

    /**
     * Given a base unit and a left context, return the set of entry points into the lex tree
     *
     * @param lc   the left context
     * @param base the center unit
     * @return the set of entry points
     */
    public Node[] getEntryPoint(Unit lc, Unit base) {
        EntryPoint ep = entryPointTable.getEntryPoint(base);
        return ep.getEntryPointsFromLeftContext(lc).getSuccessors();
    }

    /**
     * Gets the  set of hmm nodes associated with the given end node
     *
     * @param endNode the end node
     * @return an array of associated hmm nodes
     */
    public HMMNode[] getHMMNodes(EndNode endNode) {
        HMMNode[] results = endNodeMap.get(endNode.getKey());
        if (results == null) {
            // System.out.println("Filling cache for " + endNode.getKey()
            //        + " size " + endNodeMap.size());
            Map<HMM, HMMNode> resultMap = new HashMap<HMM, HMMNode>();
            Unit baseUnit = endNode.getBaseUnit();
            Unit lc = endNode.getLeftContext();
            for (Unit rc : entryPoints) {
                HMM hmm = hmmPool.getHMM(baseUnit, lc, rc, HMMPosition.END);
                HMMNode hmmNode = resultMap.get(hmm);
                if (hmmNode == null) {
                    hmmNode = new HMMNode(hmm, LogMath.LOG_ONE);
                    resultMap.put(hmm, hmmNode);
                }
                hmmNode.addRC(rc);
                for (Node node : endNode.getSuccessors()) {
                    WordNode wordNode = (WordNode) node;
                    hmmNode.addSuccessor(wordNode);
                }
            }

            // cache it
            results = resultMap.values().toArray(new HMMNode[resultMap.size()]);
            endNodeMap.put(endNode.getKey(), results);
        }

        // System.out.println("GHN: " + endNode + " " + results.length);
        return results;
    }

    /**
     * Returns the word node associated with the sentence end word
     *
     * @return the sentence end word node
     */
    public WordNode getSentenceEndWordNode() {
        assert sentenceEndWordNode != null;
        return sentenceEndWordNode;
    }

    //    private Object getKey(EndNode endNode) {
    //        Unit base = endNode.getBaseUnit();
    //        Unit lc = endNode.getLeftContext();
    //        return null;
    //    }

    /**
     * Compiles the vocabulary into an HMM Tree
     */
    private void compile() {
        collectEntryAndExitUnits();
        entryPointTable = new EntryPointTable(this, entryPoints);
        addWords();
        entryPointTable.createEntryPointMaps();
        freeze();
    }

    /**
     * Dumps the tree
     */
    void dumpTree() {
        System.out.println("Dumping Tree ...");
        Map<Node, Node> dupNode = new HashMap<Node, Node>();
        dumpTree(0, getInitialNode(), dupNode);
        System.out.println("... done Dumping Tree");
    }

    /**
     * Dumps the tree
     *
     * @param level   the level of the dump
     * @param node    the root of the tree to dump
     * @param dupNode map of visited nodes
     */
    private void dumpTree(int level, Node node, Map<Node, Node> dupNode) {
        if (dupNode.get(node) == null) {
            dupNode.put(node, node);
            System.out.println(Utilities.pad(level) + node);
            if (!(node instanceof WordNode)) {
                for (Node nextNode : node.getSuccessors()) {
                    dumpTree(level + 1, nextNode, dupNode);
                }
            }
        }
    }

    /**
     * Collects all of the entry and exit points for the vocabulary.
     */
    private void collectEntryAndExitUnits() {
        Collection<Word> words = getAllWords();
        for (Word word : words) {
            for (int j = 0; j < word.getPronunciations().length; j++) {
                Pronunciation p = word.getPronunciations()[j];
                Unit first = p.getUnits()[0];
                Unit last = p.getUnits()[p.getUnits().length - 1];
                entryPoints.add(first);
                exitPoints.add(last);
            }
        }

        if (debug) {
            System.out.println("Entry Points: " + entryPoints.size());
            System.out.println("Exit Points: " + exitPoints.size());
        }
    }

    /**
     * Called after the lex tree is built. Frees all temporary structures. After this is called, no more words can be
     * added to the lex tree.
     */
    private void freeze() {
        entryPointTable.freeze();
        dictionary = null;
        lm = null;
        exitPoints = null;
        allWords = null;
        wordNodeMap.clear();
        endNodeMap.clear();
    }

    /**
     * Adds the given collection of words to the lex tree
     */
    private void addWords() {
        Set<Word> words = getAllWords();
        for (Word word : words) {
            addWord(word);
        }
    }

    /**
     * Adds a single word to the lex tree
     *
     * @param word the word to add
     */
    private void addWord(Word word) {
        float prob = getWordUnigramProbability(word);
        Pronunciation[] pronunciations = word.getPronunciations();
        for (Pronunciation pronunciation : pronunciations) {
            addPronunciation(pronunciation, prob);
        }
    }

    /**
     * Adds the given pronunciation to the lex tree
     *
     * @param pronunciation the pronunciation
     * @param probability   the unigram probability
     */
    private void addPronunciation(Pronunciation pronunciation, float probability) {
        Unit baseUnit;
        Unit lc;
        Unit rc;
        Node curNode;
        WordNode wordNode;

        Unit[] units = pronunciation.getUnits();
        baseUnit = units[0];
        EntryPoint ep = entryPointTable.getEntryPoint(baseUnit);

        ep.addProbability(probability);

        if (units.length > 1) {
            curNode = ep.getNode();
            lc = baseUnit;
            for (int i = 1; i < units.length - 1; i++) {
                baseUnit = units[i];
                rc = units[i + 1];
                HMM hmm = hmmPool.getHMM(baseUnit, lc, rc, HMMPosition.INTERNAL);
                if (hmm == null) {
                    logger.severe(
                        "Missing HMM for unit " + baseUnit.getName() + " with lc=" + lc.getName() + " rc=" + rc
                            .getName());
                } else {
                    curNode = curNode.addSuccessor(hmm, probability);
                }
                lc = baseUnit;          // next lc is this baseUnit
            }

            // now add the last unit as an end unit
            baseUnit = units[units.length - 1];
            EndNode endNode = new EndNode(baseUnit, lc, probability);
            curNode = curNode.addSuccessor(endNode, probability);
            wordNode = curNode.addSuccessor(pronunciation, probability, wordNodeMap);
            if (wordNode.getWord().isSentenceEndWord()) {
                sentenceEndWordNode = wordNode;
            }
        } else {
            ep.addSingleUnitWord(pronunciation);
        }
    }

    /**
     * Gets the unigram probability for the given word
     *
     * @param word the word
     * @return the unigram probability for the word.
     */
    float getWordUnigramProbability(Word word) {
        float prob = LogMath.LOG_ONE;
        if (!word.isFiller()) {
            Word[] wordArray = new Word[1];
            wordArray[0] = word;
            prob = lm.getProbability((new WordSequence(wordArray)));
            // System.out.println("gwup: " + word + " " + prob);
            prob *= languageWeight;
        }
        return prob;
    }

    /**
     * Returns the entire set of words, including filler words
     *
     * @return the set of all words (as Word objects)
     */
    private Set<Word> getAllWords() {
        if (allWords == null) {
            allWords = new HashSet<Word>();
            for (String spelling : lm.getVocabulary()) {
                Word word = dictionary.getWord(spelling);
                if (word != null) {
                    allWords.add(word);
                }
            }

            if (addFillerWords) {
                allWords.addAll(Arrays.asList(dictionary.getFillerWords()));
            } else if (addSilenceWord) {
                allWords.add(dictionary.getSilenceWord());
            }
        }
        return allWords;
    }

    /**
     * Returns the initial node for this lex tree
     *
     * @return the initial lex node
     */
    public InitialWordNode getInitialNode() {
        return initialNode;
    }
}

