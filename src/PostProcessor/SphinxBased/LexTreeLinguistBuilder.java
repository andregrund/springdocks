package PostProcessor.SphinxBased;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;

public class LexTreeLinguistBuilder {

    private AcousticModel acousticModel;

    private UnitManager unitManager;

    private LanguageModel languageModel;

    private Dictionary dictionary;

    private boolean fullWordHistories;

    private boolean wantUnigramSmear;

    private double wordInsertionProbability;

    private double silenceInsertionProbability;

    private double fillerInsertionProbability;

    private double unitInsertionProbability;

    private float languageWeight;

    private boolean addFillerWords;

    private boolean generateUnitStates;

    private float unigramSmearWeight;

    private int maxArcCacheSize;

    public LexTreeLinguistBuilder acousticModel(final AcousticModel acousticModel) {
        this.acousticModel = acousticModel;
        return this;
    }

    public LexTreeLinguistBuilder unitManager(final UnitManager unitManager) {
        this.unitManager = unitManager;
        return this;
    }

    public LexTreeLinguistBuilder languageModel(final LanguageModel languageModel) {
        this.languageModel = languageModel;
        return this;
    }

    public LexTreeLinguistBuilder dictionary(final Dictionary dictionary) {
        this.dictionary = dictionary;
        return this;
    }

    public LexTreeLinguistBuilder fullWordHistories(final boolean fullWordHistories) {
        this.fullWordHistories = fullWordHistories;
        return this;
    }

    public LexTreeLinguistBuilder wantUnigramSmear(final boolean wantUnigramSmear) {
        this.wantUnigramSmear = wantUnigramSmear;
        return this;
    }

    public LexTreeLinguistBuilder wordInsertionProbability(final double wordInsertionProbability) {
        this.wordInsertionProbability = wordInsertionProbability;
        return this;
    }

    public LexTreeLinguistBuilder silenceInsertionProbability(final double silenceInsertionProbability) {
        this.silenceInsertionProbability = silenceInsertionProbability;
        return this;
    }

    public LexTreeLinguistBuilder fillerInsertionProbability(final double fillerInsertionProbability) {
        this.fillerInsertionProbability = fillerInsertionProbability;
        return this;
    }

    public LexTreeLinguistBuilder unitInsertionProbability(final double unitInsertionProbability) {
        this.unitInsertionProbability = unitInsertionProbability;
        return this;
    }

    public LexTreeLinguistBuilder languageWeight(final float languageWeight) {
        this.languageWeight = languageWeight;
        return this;
    }

    public LexTreeLinguistBuilder addFillerWords(final boolean addFillerWords) {
        this.addFillerWords = addFillerWords;
        return this;
    }

    public LexTreeLinguistBuilder generateUnitStates(final boolean generateUnitStates) {
        this.generateUnitStates = generateUnitStates;
        return this;
    }

    public LexTreeLinguistBuilder unigramSmearWeight(final float unigramSmearWeight) {
        this.unigramSmearWeight = unigramSmearWeight;
        return this;
    }

    public LexTreeLinguistBuilder maxArcCacheSize(final int maxArcCacheSize) {
        this.maxArcCacheSize = maxArcCacheSize;
        return this;
    }

    public LexTreeLinguist createLexTreeLinguist() {
        return new LexTreeLinguist(acousticModel, unitManager, languageModel, dictionary, fullWordHistories,
            wantUnigramSmear, wordInsertionProbability, silenceInsertionProbability, fillerInsertionProbability,
            unitInsertionProbability, languageWeight, addFillerWords, generateUnitStates, unigramSmearWeight,
            maxArcCacheSize);
    }
}