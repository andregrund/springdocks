package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import de.uni.hamburg.postprocessor.sphinxbased.node.EndNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.HMMNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.HMMTree;
import de.uni.hamburg.postprocessor.sphinxbased.node.InitialWordNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.Node;
import de.uni.hamburg.postprocessor.sphinxbased.node.UnitNode;
import edu.cmu.sphinx.instrumentation.MemoryTracker;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMMPool;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
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
import java.util.logging.Logger;

public class LexTreeLinguistAndre implements Linguist {

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
    final static SearchStateArc[] EMPTY_ARC = new SearchStateArc[0];

    // ----------------------------------
    // Subcomponents that are configured
    // by the property sheet
    // -----------------------------------
    LanguageModel languageModel;

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

    boolean generateUnitStates;

    private boolean wantUnigramSmear = true;

    private float unigramSmearWeight = 1.0f;

    boolean cacheEnabled;

    private int maxArcCacheSize;

    protected float languageWeight;

    private float logWordInsertionProbability;

    private float logUnitInsertionProbability;

    private float logFillerInsertionProbability;

    private float logSilenceInsertionProbability;

    float logOne;

    // ------------------------------------
    // Data used for building and maintaining
    // the search graph
    // -------------------------------------
    Word sentenceEndWord;

    private Word[] sentenceStartWordArray;

    private SearchGraph searchGraph;

    private HMMPool hmmPool;

    LRUCache<LexTreeState, SearchStateArc[]> arcCache;

    int maxDepth;

    HMMTree hmmTree;

    int cacheTrys;

    int cacheHits;

    public LexTreeLinguistAndre(AcousticModel acousticModel, UnitManager unitManager, LanguageModel languageModel,
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

    public LexTreeLinguistAndre() {

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

        return new LexTreeWordSearchState(this, node, node.getParent(),
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

    /**
     * Determines the insertion probability for the given unit lex node
     *
     * @param unitNode the unit lex node
     * @return the insertion probability
     */
    float calculateInsertionProbability(final UnitNode unitNode) {
        final MemoryTracker memoryTracker = new MemoryTracker();

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
    float getUnigramSmear(Node node) {
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
    float getSmearTermFromLanguageModel(WordSequence ws) {
        return languageModel.getSmear(ws);
    }

    /**
     * Gets the set of HMM nodes associated with the given end node
     *
     * @param endNode the end node
     * @return an array of associated HMM nodes
     */
    HMMNode[] getHMMNodes(EndNode endNode) {
        return hmmTree.getHMMNodes(endNode);
    }
}
