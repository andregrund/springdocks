package de.uni.hamburg.postprocessor.sphinxbased;

import edu.cmu.sphinx.decoder.scorer.ScoreProvider;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.HMMSearchState;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.UnitSearchState;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPool;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.HMMStateArc;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.linguist.util.LRUCache;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4Integer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AndresLexTreeLinguist implements Linguist {

    /**
     * The property that defines the grammar to use when building the search graph
     */
    @S4Component(type = Grammar.class)
    private static final String PROP_GRAMMAR = "grammar";

    /**
     * The property that defines the acoustic model to use when building the search graph
     */
    @S4Component(type = AcousticModel.class)
    private static final String PROP_ACOUSTIC_MODEL = "acousticModel";

    /**
     * The property that defines the unit manager to use when building the search graph
     */
    @S4Component(type = UnitManager.class, defaultClass = UnitManager.class)
    private static final String PROP_UNIT_MANAGER = "unitManager";

    /**
     * The property that determines whether or not full word histories are used to
     * determine when two states are equal.
     */
    @S4Boolean(defaultValue = true)
    private static final String PROP_FULL_WORD_HISTORIES = "fullWordHistories";

    /**
     * The property for the language model to be used by this grammar
     */
    @S4Component(type = LanguageModel.class)
    private static final String PROP_LANGUAGE_MODEL = "languageModel";

    /**
     * The property that defines the dictionary to use for this grammar
     */
    @S4Component(type = Dictionary.class)
    private static final String PROP_DICTIONARY = "dictionary";

    /**
     * The property that defines the size of the arc cache (zero to disable the cache).
     */
    @S4Integer(defaultValue = 0)
    private static final String PROP_CACHE_SIZE = "cacheSize";

    /**
     * The property that controls whether filler words are automatically added to the vocabulary
     */
    @S4Boolean(defaultValue = false)
    private static final String PROP_ADD_FILLER_WORDS = "addFillerWords";

    /**
     * The property to control whether or not the linguist will generate unit states.   When this property is false the
     * linguist may omit UnitSearchState states.  For some search algorithms this will allow for a faster search with
     * more compact results.
     */
    @S4Boolean(defaultValue = false)
    private static final String PROP_GENERATE_UNIT_STATES = "generateUnitStates";

    /**
     * The property that determines whether or not unigram probabilities are
     * smeared through the lextree. During the expansion of the tree the
     * language probability could be only calculated when we reach word end node.
     * Until that point we need to keep path alive and give it some language
     * probability. See
     * Alleva, F., Huang, X. and Hwang, M.-Y., "Improvements on the pronunciation
     * prefix tree search organization", Proceedings of ICASSP, pp. 133-136,
     * Atlanta, GA, 1996.
     * for the description of this technique.
     */
    @S4Boolean(defaultValue = true)
    private static final String PROP_WANT_UNIGRAM_SMEAR = "wantUnigramSmear";

    /**
     * The property that determines the weight of the smear. See {@link edu.cmu.sphinx.linguist.lextree.LexTreeLinguist#PROP_WANT_UNIGRAM_SMEAR}
     */
    @S4Double(defaultValue = 1.0)
    private static final String PROP_UNIGRAM_SMEAR_WEIGHT = "unigramSmearWeight";

    // just for detailed debugging
    private final static SearchStateArc[] EMPTY_ARC = new SearchStateArc[0];

    // ----------------------------------
    // Subcomponents that are configured
    // by the property sheet
    // -----------------------------------
    private LanguageModel languageModel;

    private AcousticModel acousticModel;

    private LogMath logMath;

    private Dictionary dictionary;

    private UnitManager unitManager;

    // ------------------------------------
    // Data that is configured by the
    // property sheet
    // ------------------------------------
    private Logger logger;

    private boolean addFillerWords;

    private boolean generateUnitStates;

    private boolean wantUnigramSmear = true;

    private float unigramSmearWeight = 1.0f;

    private boolean cacheEnabled;

    private int maxArcCacheSize;

    protected float languageWeight;

    private float logWordInsertionProbability;

    private float logUnitInsertionProbability;

    private float logFillerInsertionProbability;

    private float logSilenceInsertionProbability;

    private float logOne;

    // ------------------------------------
    // Data used for building and maintaining
    // the search graph
    // -------------------------------------
    private Word sentenceEndWord;

    private Word[] sentenceStartWordArray;

    private SearchGraph searchGraph;

    private HMMPool hmmPool;

    private LRUCache<LexTreeState, SearchStateArc[]> arcCache;

    private int maxDepth;

    private HMMTree hmmTree;

    private int cacheTrys;

    private int cacheHits;

    public AndresLexTreeLinguist(AcousticModel acousticModel, UnitManager unitManager, LanguageModel languageModel,
        Dictionary dictionary, boolean fullWordHistories, boolean wantUnigramSmear, double wordInsertionProbability,
        double silenceInsertionProbability, double fillerInsertionProbability, double unitInsertionProbability,
        float languageWeight, boolean addFillerWords, boolean generateUnitStates, float unigramSmearWeight,
        int maxArcCacheSize) {

        logger = Logger.getLogger(getClass().getName());

        this.acousticModel = acousticModel;
        this.logMath = LogMath.getLogMath();
        this.unitManager = unitManager;
        this.languageModel = languageModel;
        this.dictionary = dictionary;

        this.wantUnigramSmear = wantUnigramSmear;
        this.logWordInsertionProbability = logMath.linearToLog(wordInsertionProbability);
        this.logSilenceInsertionProbability = logMath.linearToLog(silenceInsertionProbability);
        this.logFillerInsertionProbability = logMath.linearToLog(fillerInsertionProbability);
        this.logUnitInsertionProbability = logMath.linearToLog(unitInsertionProbability);
        this.languageWeight = languageWeight;
        this.addFillerWords = addFillerWords;
        this.generateUnitStates = generateUnitStates;
        this.unigramSmearWeight = unigramSmearWeight;
        this.maxArcCacheSize = maxArcCacheSize;

        cacheEnabled = maxArcCacheSize > 0;
        if (cacheEnabled) {
            arcCache = new LRUCache<LexTreeState, SearchStateArc[]>(maxArcCacheSize);
        }
    }

    public AndresLexTreeLinguist() {

    }

    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        logger = ps.getLogger();
        logMath = LogMath.getLogMath();

        acousticModel = (AcousticModel) ps.getComponent(PROP_ACOUSTIC_MODEL);
        unitManager = (UnitManager) ps.getComponent(PROP_UNIT_MANAGER);
        languageModel = (LanguageModel) ps.getComponent(PROP_LANGUAGE_MODEL);
        dictionary = (Dictionary) ps.getComponent(PROP_DICTIONARY);

        wantUnigramSmear = ps.getBoolean(PROP_WANT_UNIGRAM_SMEAR);
        logWordInsertionProbability = logMath.linearToLog(ps.getDouble(PROP_WORD_INSERTION_PROBABILITY));
        logSilenceInsertionProbability = logMath.linearToLog(ps.getDouble(PROP_SILENCE_INSERTION_PROBABILITY));
        logFillerInsertionProbability = logMath.linearToLog(ps.getDouble(PROP_FILLER_INSERTION_PROBABILITY));
        logUnitInsertionProbability = logMath.linearToLog(ps.getDouble(PROP_UNIT_INSERTION_PROBABILITY));
        languageWeight = ps.getFloat(PROP_LANGUAGE_WEIGHT);
        addFillerWords = (ps.getBoolean(PROP_ADD_FILLER_WORDS));
        generateUnitStates = (ps.getBoolean(PROP_GENERATE_UNIT_STATES));
        unigramSmearWeight = ps.getFloat(PROP_UNIGRAM_SMEAR_WEIGHT);
        maxArcCacheSize = ps.getInt(PROP_CACHE_SIZE);

        cacheEnabled = maxArcCacheSize > 0;
        if (cacheEnabled) {
            arcCache = new LRUCache<>(maxArcCacheSize);
        }
    }

    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.linguist.Linguist#allocate()
    */
    public void allocate() throws IOException {
        dictionary.allocate();
        acousticModel.allocate();
        languageModel.allocate();
        compileGrammar();
    }

    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.linguist.Linguist#deallocate()
    */
    public void deallocate() throws IOException {
        if (acousticModel != null)
            acousticModel.deallocate();
        if (dictionary != null)
            dictionary.deallocate();
        if (languageModel != null)
            languageModel.deallocate();
        hmmTree = null;
    }

    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.linguist.Linguist#getSearchGraph()
    */
    public SearchGraph getSearchGraph() {
        return searchGraph;
    }

    /**
     * Called before a recognition
     */
    public void startRecognition() {
    }

    /**
     * Called after a recognition
     */
    public void stopRecognition() {
        languageModel.onUtteranceEnd();
    }

    /**
     * Retrieves the language model for this linguist
     *
     * @return the language model (or null if there is none)
     */
    public LanguageModel getLanguageModel() {
        return languageModel;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * retrieves the initial language state
     *
     * @return the initial language state
     */
    private SearchState getInitialSearchState() {
        InitialWordNode node = hmmTree.getInitialNode();

        if (node == null)
            throw new RuntimeException("Language model has no entry for initial word <s>");

        return new LexTreeWordState(node, node.getParent(),
            (new WordSequence(sentenceStartWordArray)).trim(maxDepth - 1), 0f, logOne, logOne);
    }

    /**
     * Compiles the n-gram into a lex tree that is used during the search
     */
    private void compileGrammar() {
        TimerPool.getTimer(this, "Compile").start();

        sentenceEndWord = dictionary.getSentenceEndWord();
        sentenceStartWordArray = new Word[1];
        sentenceStartWordArray[0] = dictionary.getSentenceStartWord();
        maxDepth = languageModel.getMaxDepth();

        generateHmmTree();

        TimerPool.getTimer(this, "Compile").stop();
        // Now that we are all done, dump out some interesting
        // information about the process

        searchGraph = new LexTreeSearchGraph(getInitialSearchState());
    }

    protected void generateHmmTree() {
        hmmPool = new HMMPool(acousticModel, logger, unitManager);
        hmmTree = new HMMTree(hmmPool, dictionary, languageModel, addFillerWords, languageWeight);

        hmmPool.dumpInfo();
    }

    class LexTreeSearchGraph implements SearchGraph {

        /**
         * An array of classes that represents the order in which the states will be returned.
         */

        private SearchState initialState;

        /**
         * Constructs a search graph with the given initial state
         *
         * @param initialState the initial state
         */
        LexTreeSearchGraph(SearchState initialState) {
            this.initialState = initialState;
        }

        /*
        * (non-Javadoc)
        *
        * @see edu.cmu.sphinx.linguist.SearchGraph#getInitialState()
        */
        public SearchState getInitialState() {
            return initialState;
        }

        /*
        * (non-Javadoc)
        *
        * @see edu.cmu.sphinx.linguist.SearchGraph#getSearchStateOrder()
        */
        public int getNumStateOrder() {
            return 6;
        }

        public boolean getWordTokenFirst() {
            return false;
        }
    }

    /**
     * The LexTreeLinguist returns language states to the search manager. This class forms the base implementation for
     * all language states returned. This LexTreeState keeps track of the probability of entering this state (a
     * language+insertion probability) as well as the unit history. The unit history consists of the LexTree nodes that
     * correspond to the left, center and right contexts.
     * This is an abstract class, subclasses must implement the getSuccessorss method.
     */
    abstract class LexTreeState implements SearchState, SearchStateArc {

        private final Node node;

        private final WordSequence wordSequence;

        final float currentSmearTerm;

        final float currentSmearProb;

        /**
         * Creates a LexTreeState.
         *
         * @param node         the node associated with this state
         * @param wordSequence the history of words up until this point
         */
        LexTreeState(Node node, WordSequence wordSequence, float smearTerm, float smearProb) {
            this.node = node;
            this.wordSequence = wordSequence;
            currentSmearTerm = smearTerm;
            currentSmearProb = smearProb;
        }

        /**
         * Gets the unique signature for this state. The signature building code is slow and should only be used for
         * non-time-critical tasks such as plotting states.
         *
         * @return the signature
         */
        public String getSignature() {
            return "lts-" + node.hashCode() + "-ws-" + wordSequence;
        }

        public float getSmearTerm() {
            return currentSmearTerm;
        }

        public float getSmearProb() {
            return currentSmearProb;
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            int hashCode = wordSequence.hashCode() * 37;
            hashCode += node.hashCode();
            return hashCode;
        }

        /**
         * Determines if the given object is equal to this object
         *
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeState) {
                LexTreeState other = (LexTreeState) o;
                if (node != other.node)
                    return false;
                return wordSequence.equals(other.wordSequence);
            } else {
                return false;
            }
        }

        /**
         * Gets a successor to this search state
         *
         * @return the successor state
         */
        public SearchState getState() {
            return this;
        }

        /**
         * Gets the composite probability of entering this state
         *
         * @return the log probability
         */
        public float getProbability() {
            return getLanguageProbability() + getInsertionProbability();
        }

        /**
         * Gets the language probability of entering this state
         *
         * @return the log probability
         */
        public float getLanguageProbability() {
            return logOne;
        }

        /**
         * Gets the insertion probability of entering this state
         *
         * @return the log probability
         */
        public float getInsertionProbability() {
            return logOne;
        }

        /**
         * Determines if this is an emitting state
         *
         * @return <code>true</code> if this is an emitting state.
         */
        public boolean isEmitting() {
            return false;
        }

        /**
         * Determines if this is a final state
         *
         * @return <code>true</code> if this is an final state.
         */
        public boolean isFinal() {
            return false;
        }

        /**
         * Gets the hmm tree node representing the unit
         *
         * @return the unit lex node
         */
        protected Node getNode() {
            return node;
        }

        /**
         * Returns the word sequence for this state
         *
         * @return the word sequence
         */
        public WordSequence getWordHistory() {
            return wordSequence;
        }

        public Node getLexState() {
            return node;
        }

        /**
         * Returns the list of successors to this state
         *
         * @return a list of SearchState objects
         */
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs = getCachedArcs();
            if (arcs == null) {
                arcs = getSuccessors(node);
                putCachedArcs(arcs);
            }
            return arcs;
        }

        /**
         * Returns the list of successors to this state
         *
         * @param theNode node to get successors
         * @return a list of SearchState objects
         */
        protected SearchStateArc[] getSuccessors(Node theNode) {
            Node[] nodes = theNode.getSuccessors();
            SearchStateArc[] arcs = new SearchStateArc[nodes.length];
            // System.out.println("Arc: "+ this);
            int i = 0;
            for (Node nextNode : nodes) {
                //  System.out.println(" " + nextNode);
                if (nextNode instanceof WordNode) {
                    arcs[i] = createWordStateArc((WordNode) nextNode, (HMMNode) getNode(), this);
                } else if (nextNode instanceof EndNode) {
                    arcs[i] = createEndUnitArc((EndNode) nextNode, this);
                } else {
                    arcs[i] = createUnitStateArc((HMMNode) nextNode, this);
                }
                i++;
            }
            return arcs;
        }

        /**
         * Creates a word search state for the given word node
         *
         * @param wordNode the wordNode
         * @param lastUnit last unit of the word
         * @param previous previous state
         * @return the search state for the wordNode
         */
        protected SearchStateArc createWordStateArc(WordNode wordNode, HMMNode lastUnit, LexTreeState previous) {
            // System.out.println("CWSA " + wordNode + " fup " + fixupProb);
            float languageProbability = logOne;
            Word nextWord = wordNode.getWord();
            float smearTerm = previous.getSmearTerm();

            if (nextWord.isFiller() && nextWord != sentenceEndWord) {
                return new LexTreeWordState(wordNode, lastUnit, wordSequence, smearTerm, logOne, languageProbability);
            }

            WordSequence nextWordSequence = wordSequence.addWord(nextWord, maxDepth);
            float probability = languageModel.getProbability(nextWordSequence) * languageWeight;
            smearTerm = getSmearTermFromLanguageModel(nextWordSequence);
            // System.out.println("LP " + nextWordSequence + " " +
            // logProbability);
            // subtract off the previously applied smear probability
            languageProbability = probability - previous.getSmearProb();

            if (nextWord == sentenceEndWord) {
                return new LexTreeEndWordState(wordNode, lastUnit, nextWordSequence.trim(maxDepth - 1), smearTerm,
                    logOne, languageProbability);
            }

            return new LexTreeWordState(wordNode, lastUnit, nextWordSequence.trim(maxDepth - 1), smearTerm, logOne,
                languageProbability);
        }

        /**
         * Creates a unit search state for the given unit node
         *
         * @param hmmNode the unit node
         * @return the search state
         */
        SearchStateArc createUnitStateArc(HMMNode hmmNode, LexTreeState previous) {
            SearchStateArc arc;
            // System.out.println("CUSA " + hmmNode);
            float insertionProbability = calculateInsertionProbability(hmmNode);
            float smearProbability = getUnigramSmear(hmmNode) + previous.getSmearTerm();
            float languageProbability = smearProbability - previous.getSmearProb();

            // if we want a unit state create it, otherwise
            // get the first hmm state of the unit

            if (generateUnitStates) {
                arc = new LexTreeUnitState(hmmNode, getWordHistory(), previous.getSmearTerm(), smearProbability,
                    languageProbability, insertionProbability);
            } else {
                HMM hmm = hmmNode.getHMM();
                arc = new LexTreeHMMState(hmmNode, getWordHistory(), previous.getSmearTerm(), smearProbability,
                    hmm.getInitialState(), languageProbability, insertionProbability, null);
            }
            return arc;
        }

        /**
         * Creates a unit search state for the given unit node
         *
         * @param endNode  the unit node
         * @param previous the previous state
         * @return the search state
         */
        SearchStateArc createEndUnitArc(EndNode endNode, LexTreeState previous) {
            float smearProbability = getUnigramSmear(endNode) + previous.getSmearTerm();
            float languageProbability = smearProbability - previous.getSmearProb();
            float insertionProbability = calculateInsertionProbability(endNode);
            return new LexTreeEndUnitState(endNode, getWordHistory(), previous.getSmearTerm(), smearProbability,
                languageProbability, insertionProbability);
        }

        /**
         * Returns the string representation of this object
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "lt-" + node + ' ' + getProbability() + '{' + wordSequence + '}';
        }

        /**
         * Returns a pretty version of the string representation for this object
         *
         * @return a pretty string
         */
        public String toPrettyString() {
            return toString();
        }

        /**
         * Gets the successor arcs for this state from the cache
         *
         * @return the next set of arcs for this state, or null if none can be found or if caching is disabled.
         */
        SearchStateArc[] getCachedArcs() {
            if (cacheEnabled) {
                SearchStateArc[] arcs = arcCache.get(this);
                if (arcs != null) {
                    cacheHits++;
                }
                if (++cacheTrys % 1000000 == 0) {
                    System.out.println(
                        "Hits: " + cacheHits + " of " + cacheTrys + ' ' + ((float) cacheHits) / cacheTrys * 100f);
                }
                return arcs;
            } else {
                return null;
            }
        }

        /**
         * Puts the set of arcs into the cache
         *
         * @param arcs the arcs to cache.
         */
        void putCachedArcs(SearchStateArc[] arcs) {
            if (cacheEnabled) {
                arcCache.put(this, arcs);
            }
        }

        abstract public int getOrder();
    }

    /**
     * Represents a unit in the search space
     */
    public class LexTreeEndUnitState extends LexTreeState implements UnitSearchState {

        float logLanguageProbability;

        float logInsertionProbability;

        /**
         * Constructs a LexTreeUnitState
         *
         * @param wordSequence the history of words
         */
        LexTreeEndUnitState(EndNode endNode, WordSequence wordSequence, float smearTerm, float smearProb,
            float languageProbability, float insertionProbability) {

            super(endNode, wordSequence, smearTerm, smearProb);
            logLanguageProbability = languageProbability;
            logInsertionProbability = insertionProbability;
            // System.out.println("LTEUS " + logLanguageProbability + " " +
            // logInsertionProbability);
        }

        /**
         * Returns the base unit associated with this state
         *
         * @return the base unit
         */
        public Unit getUnit() {
            return getEndNode().getBaseUnit();
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            return super.hashCode() * 17 + 423;
        }

        /**
         * Gets the acoustic probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getInsertionProbability() {
            return logInsertionProbability;
        }

        /**
         * Gets the language probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getLanguageProbability() {
            return logLanguageProbability;
        }

        /**
         * Determines if the given object is equal to this object
         *
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof LexTreeEndUnitState && super.equals(o);
        }

        /**
         * Returns the unit node for this state
         *
         * @return the unit node
         */
        private EndNode getEndNode() {
            return (EndNode) getNode();
        }

        /**
         * Returns the list of successors to this state
         *
         * @return a list of SearchState objects
         */
        @Override
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs = getCachedArcs();
            if (arcs == null) {
                HMMNode[] nodes = getHMMNodes(getEndNode());
                arcs = new SearchStateArc[nodes.length];

                if (generateUnitStates) {
                    for (int i = 0; i < nodes.length; i++) {
                        arcs[i] = new LexTreeUnitState(nodes[i], getWordHistory(), getSmearTerm(), getSmearProb(),
                            logOne, logOne, this.getNode());
                    }
                } else {
                    for (int i = 0; i < nodes.length; i++) {
                        HMM hmm = nodes[i].getHMM();
                        arcs[i] = new LexTreeHMMState(nodes[i], getWordHistory(), getSmearTerm(), getSmearProb(),
                            hmm.getInitialState(), logOne, logOne, this.getNode());
                    }
                }
                putCachedArcs(arcs);
            }
            return arcs;
        }

        @Override
        public String toString() {
            return super.toString() + " EndUnit";
        }

        @Override
        public int getOrder() {
            return 3;
        }
    }

    /**
     * Represents a unit in the search space
     */
    public class LexTreeUnitState extends LexTreeState implements UnitSearchState {

        private float logInsertionProbability;

        private float logLanguageProbability;

        private Node parentNode;

        private int hashCode = -1;

        /**
         * Constructs a LexTreeUnitState
         *
         * @param wordSequence the history of words
         */
        LexTreeUnitState(HMMNode hmmNode, WordSequence wordSequence, float smearTerm, float smearProb,
            float languageProbability, float insertionProbability) {
            this(hmmNode, wordSequence, smearTerm, smearProb, languageProbability, insertionProbability, null);
        }

        /**
         * Constructs a LexTreeUnitState
         *
         * @param wordSequence the history of words
         */
        LexTreeUnitState(HMMNode hmmNode, WordSequence wordSequence, float smearTerm, float smearProb,
            float languageProbability, float insertionProbability, Node parentNode) {
            super(hmmNode, wordSequence, smearTerm, smearProb);
            this.logInsertionProbability = insertionProbability;
            this.logLanguageProbability = languageProbability;
            this.parentNode = parentNode;

        }

        /**
         * Returns the base unit associated with this state
         *
         * @return the base unit
         */
        public Unit getUnit() {
            return getHMMNode().getBaseUnit();
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            if (hashCode == -1) {
                hashCode = super.hashCode() * 17 + 421;
                if (parentNode != null) {
                    hashCode *= 432;
                    hashCode += parentNode.hashCode();
                }
            }
            return hashCode;
        }

        /**
         * Determines if the given object is equal to this object
         *
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeUnitState) {
                LexTreeUnitState other = (LexTreeUnitState) o;
                return parentNode == other.parentNode && super.equals(o);
            } else {
                return false;
            }
        }

        /**
         * Returns the unit node for this state
         *
         * @return the unit node
         */
        private HMMNode getHMMNode() {
            return (HMMNode) getNode();
        }

        /**
         * Returns the list of successors to this state
         *
         * @return a list of SearchState objects
         */
        @Override
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs = new SearchStateArc[1];
            HMM hmm = getHMMNode().getHMM();
            arcs[0] = new LexTreeHMMState(getHMMNode(), getWordHistory(), getSmearTerm(), getSmearProb(),
                hmm.getInitialState(), logOne, logOne, parentNode);
            return arcs;
        }

        @Override
        public String toString() {
            return super.toString() + " unit";
        }

        /**
         * Gets the acoustic probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getInsertionProbability() {
            return logInsertionProbability;
        }

        /**
         * Gets the language probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getLanguageProbability() {
            return logLanguageProbability;
        }

        @Override
        public int getOrder() {
            return 4;
        }
    }

    /**
     * Represents a HMM state in the search space
     */
    public class LexTreeHMMState extends LexTreeState implements HMMSearchState, ScoreProvider {

        private final HMMState hmmState;

        private float logLanguageProbability;

        private float logInsertionProbability;

        private final Node parentNode;

        int hashCode = -1;

        /**
         * Constructs a LexTreeHMMState
         *
         * @param hmmNode              the HMM state associated with this unit
         * @param wordSequence         the word history
         * @param languageProbability  the probability of the transition
         * @param insertionProbability the probability of the transition
         */
        LexTreeHMMState(HMMNode hmmNode, WordSequence wordSequence, float smearTerm, float smearProb, HMMState hmmState,
            float languageProbability, float insertionProbability, Node parentNode) {
            super(hmmNode, wordSequence, smearTerm, smearProb);
            this.hmmState = hmmState;
            this.parentNode = parentNode;
            this.logLanguageProbability = languageProbability;
            this.logInsertionProbability = insertionProbability;
        }

        /**
         * Gets the ID for this state
         *
         * @return the ID
         */
        @Override
        public String getSignature() {
            return super.getSignature() + "-HMM-" + hmmState.getState();
        }

        /**
         * returns the HMM state associated with this state
         *
         * @return the HMM state
         */
        public HMMState getHMMState() {
            return hmmState;
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            if (hashCode == -1) {
                hashCode = super.hashCode() * 29 + (hmmState.getState() + 1);
                if (parentNode != null) {
                    hashCode *= 377;
                    hashCode += parentNode.hashCode();
                }
            }
            return hashCode;
        }

        /**
         * Determines if the given object is equal to this object
         *
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeHMMState) {
                LexTreeHMMState other = (LexTreeHMMState) o;
                return hmmState == other.hmmState && parentNode == other.parentNode && super.equals(o);
            } else {
                return false;
            }
        }

        /**
         * Gets the language probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getLanguageProbability() {
            return logLanguageProbability;
        }

        /**
         * Gets the language probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getInsertionProbability() {
            return logInsertionProbability;
        }

        /**
         * Retrieves the set of successors for this state
         *
         * @return the list of successor states
         */
        @Override
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] nextStates = getCachedArcs();
            if (nextStates == null) {

                // if this is an exit state, we are transitioning to a
                // new unit or to a word end.

                if (hmmState.isExitState()) {
                    if (parentNode == null) {
                        nextStates = super.getSuccessors();
                    } else {
                        nextStates = super.getSuccessors(parentNode);
                    }
                } else {
                    // The current hmm state is not an exit state, so we
                    // just go through the next set of successors

                    HMMStateArc[] arcs = hmmState.getSuccessors();
                    nextStates = new SearchStateArc[arcs.length];
                    for (int i = 0; i < arcs.length; i++) {
                        HMMStateArc arc = arcs[i];
                        if (arc.getHMMState().isEmitting()) {
                            // if its a self loop and the prob. matches
                            // reuse the state
                            if (arc.getHMMState() == hmmState && logInsertionProbability == arc.getLogProbability()) {
                                nextStates[i] = this;
                            } else {
                                nextStates[i] = new LexTreeHMMState((HMMNode) getNode(), getWordHistory(),
                                    getSmearTerm(), getSmearProb(), arc.getHMMState(), logOne, arc.getLogProbability(),
                                    parentNode);
                            }
                        } else {
                            nextStates[i] = new LexTreeNonEmittingHMMState((HMMNode) getNode(), getWordHistory(),
                                getSmearTerm(), getSmearProb(), arc.getHMMState(), arc.getLogProbability(), parentNode);
                        }
                    }
                }
                putCachedArcs(nextStates);
            }
            return nextStates;
        }

        /**
         * Determines if this is an emitting state
         */
        @Override
        public boolean isEmitting() {
            return hmmState.isEmitting();
        }

        @Override
        public String toString() {
            return super.toString() + " hmm:" + hmmState;
        }

        @Override
        public int getOrder() {
            return 5;
        }

        public float getScore(Data data) {
            return hmmState.getScore(data);
        }

        public float[] getComponentScore(Data feature) {
            return hmmState.calculateComponentScore(feature);
        }

    }

    /**
     * Represents a non emitting hmm state
     */
    public class LexTreeNonEmittingHMMState extends LexTreeHMMState {

        /**
         * Constructs a NonEmittingLexTreeHMMState
         *
         * @param hmmState     the hmm state associated with this unit
         * @param wordSequence the word history
         * @param probability  the probability of the transition occurring
         */
        LexTreeNonEmittingHMMState(HMMNode hmmNode, WordSequence wordSequence, float smearTerm, float smearProb,
            HMMState hmmState, float probability, Node parentNode) {
            super(hmmNode, wordSequence, smearTerm, smearProb, hmmState, logOne, probability, parentNode);
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    /**
     * Represents a word state in the search space
     */
    public class LexTreeWordState extends LexTreeState implements WordSearchState {

        private HMMNode lastNode;

        private float logLanguageProbability;

        /**
         * Constructs a LexTreeWordState
         *
         * @param wordNode            the word node
         * @param wordSequence        the sequence of words triphone context
         * @param languageProbability the probability of this word
         */
        LexTreeWordState(WordNode wordNode, HMMNode lastNode, WordSequence wordSequence, float smearTerm,
            float smearProb, float languageProbability) {

            super(wordNode, wordSequence, smearTerm, smearProb);
            // System.out.println("LTWS " + wordSequence);
            this.lastNode = lastNode;
            this.logLanguageProbability = languageProbability;
        }

        /**
         * Gets the word pronunciation for this state
         *
         * @return the pronunciation for this word
         */
        public Pronunciation getPronunciation() {
            return ((WordNode) getNode()).getPronunciation();
        }

        /**
         * Determines if this is a final state
         *
         * @return <code>true</code> if this is an final state.
         */
        @Override
        public boolean isFinal() {
            return ((WordNode) getNode()).isFinal();
        }

        /**
         * Generate a hashcode for an object
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            return super.hashCode() * 41 + lastNode.hashCode();
        }

        /**
         * Gets the unique signature for this state. The signature building code is slow and should only be used for
         * non-time-critical tasks such as plotting states.
         *
         * @return the signature
         */
        @Override
        public String getSignature() {
            return super.getSignature() + "-ln-" + lastNode.hashCode();
        }

        /**
         * Determines if the given object is equal to this object
         *
         * @param o the object to test
         * @return <code>true</code> if the object is equal to this
         */
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LexTreeWordState) {
                LexTreeWordState other = (LexTreeWordState) o;
                return lastNode == other.lastNode && super.equals(o);
            } else {
                return false;
            }
        }

        /**
         * Gets the language probability of entering this state
         *
         * @return the log probability
         */
        @Override
        public float getLanguageProbability() {
            return logLanguageProbability;
        }

        /**
         * Returns the list of successors to this state
         *
         * @return a list of SearchState objects
         */
        @Override
        public SearchStateArc[] getSuccessors() {
            SearchStateArc[] arcs = getCachedArcs();
            if (arcs == null) {
                arcs = EMPTY_ARC;
                WordNode wordNode = (WordNode) getNode();

                if (wordNode.getWord() != sentenceEndWord) {
                    int index = 0;
                    List<Node> list = new ArrayList<Node>();
                    Unit[] rc = lastNode.getRC();
                    Unit left = wordNode.getLastUnit();

                    for (Unit unit : rc) {
                        Node[] epList = hmmTree.getEntryPoint(left, unit);
                        for (Node n : epList) {
                            list.add(n);
                        }
                    }

                    // add a link to every possible entry point as well
                    // as link to the </s> node
                    arcs = new SearchStateArc[list.size() + 1];
                    for (Node node : list) {
                        arcs[index++] = createUnitStateArc((HMMNode) node, this);
                    }

                    // now add the link to the end of sentence arc:

                    arcs[index++] = createWordStateArc(hmmTree.getSentenceEndWordNode(), lastNode, this);
                }
                putCachedArcs(arcs);
            }
            return arcs;
        }

        @Override
        public int getOrder() {
            return 1;
        }

        /**
         * Returns true if this LexTreeWordState indicates the start of a word. Returns false if this LexTreeWordState
         * indicates the end of a word.
         *
         * @return true if this LexTreeWordState indicates the start of a word, false if this LexTreeWordState indicates
         * the end of a word
         */
        public boolean isWordStart() {
            return false;
        }
    }

    /**
     * Represents the final end of utterance word
     */
    public class LexTreeEndWordState extends LexTreeWordState implements WordSearchState {

        /**
         * Constructs a LexTreeWordState
         *
         * @param wordNode       the word node
         * @param lastNode       the previous word node
         * @param wordSequence   the sequence of words triphone context
         * @param logProbability the probability of this word occurring
         */
        LexTreeEndWordState(WordNode wordNode, HMMNode lastNode, WordSequence wordSequence, float smearTerm,
            float smearProb, float logProbability) {
            super(wordNode, lastNode, wordSequence, smearTerm, smearProb, logProbability);
        }

        @Override
        public int getOrder() {
            return 2;
        }

    }

    /**
     * Determines the insertion probability for the given unit lex node
     *
     * @param unitNode the unit lex node
     * @return the insertion probability
     */
    private float calculateInsertionProbability(UnitNode unitNode) {
        int type = unitNode.getType();

        if (type == UnitNode.SIMPLE_UNIT) {
            return logUnitInsertionProbability;
        } else if (type == UnitNode.WORD_BEGINNING_UNIT) {
            return logUnitInsertionProbability + logWordInsertionProbability;
        } else if (type == UnitNode.SILENCE_UNIT) {
            return logSilenceInsertionProbability;
        } else { // must be filler
            return logFillerInsertionProbability;
        }
    }

    /**
     * Retrieves the unigram smear from the given node
     *
     * @return the unigram smear
     */
    private float getUnigramSmear(Node node) {
        float prob;
        if (wantUnigramSmear) {
            prob = node.getUnigramProbability() * unigramSmearWeight;
        } else {
            prob = logOne;
        }
        return prob;
    }

    /**
     * Returns the smear term for the given word sequence
     *
     * @param ws the word sequence
     * @return the smear term for the word sequence
     */
    private float getSmearTermFromLanguageModel(WordSequence ws) {
        return languageModel.getSmear(ws);
    }

    /**
     * Gets the set of HMM nodes associated with the given end node
     *
     * @param endNode the end node
     * @return an array of associated HMM nodes
     */
    private HMMNode[] getHMMNodes(EndNode endNode) {
        return hmmTree.getHMMNodes(endNode);
    }
}
