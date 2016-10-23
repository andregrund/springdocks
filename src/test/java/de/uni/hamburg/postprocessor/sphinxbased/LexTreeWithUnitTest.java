package de.uni.hamburg.postprocessor.sphinxbased;

import de.uni.hamburg.postprocessor.sphinxbased.node.HMMNode;
import de.uni.hamburg.postprocessor.sphinxbased.node.Node;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMMState;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.util.LogMath;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Ignore
@RunWith(JMockit.class)
public class LexTreeWithUnitTest {

    private static final String LEX_TREE_HMM_STATE_CLASS_NAME = "LexTreeHMMState";
    private static final String LEX_TREE_STATE_CLASS_NAME = "LexTreeState";

    @Tested
    private LexTreeLinguist lexTreeLinguist;

    @Mocked
    private AcousticModel acousticModel;

    @Mocked
    private UnitManager unitManager;

    @Mocked
    private LanguageModel languageModel;

    @Mocked
    private Dictionary dictionary;

    @Mocked
    private LogMath logMath;

    private SenoneHMMState lexTreeHmmState;

    @Test
    public void testLexTreeGetScore(@Mocked HMMState hmmState, @Mocked Data data, @Mocked HMMNode hmmNode,
        @Mocked WordSequence wordSequence, @Mocked Node parentNode) throws Exception {
        new Expectations() {{
            //@formatter:off
            hmmState.getScore((Data) any); result = 3.4F;
            //@formatter:on

        }};

        final float smearTerm = 1F;
        final float smearProb = 2F;
        final float languageProbability = 3F;
        final float insertionProbability = 4F;

        final LexTreeLinguist.LexTreeHMMState lexTreeState = Deencapsulation
            .newInnerInstance(LEX_TREE_HMM_STATE_CLASS_NAME, lexTreeLinguist, hmmNode, wordSequence, smearTerm,
                smearProb, hmmState, languageProbability, insertionProbability, parentNode);

        final float score = lexTreeState.getScore(data);

        assertThat(score, is(3.4F));
        // Aufbau HMMState
        //        HMMState
        //          -> HMM
        //              -> Unit (HH(DH,OW)), BaseUnit (HH)
        //          -> isEmitting
        //        ==> getScore(Data data) gibt cached score von Senone zurueck
    }

    @Test
    public void testLexTreeGetSuccessorsNotNullNoCachedArcs(@Mocked Node parentNode, @Mocked HMMState hmmState,
        @Mocked HMMNode hmmNode, @Mocked WordSequence wordSequence) throws Exception {
        final float smearTerm = 1F;
        final float smearProb = 2F;
        final float languageProbability = 3F;
        final float insertionProbability = 4F;
        final LexTreeLinguist.LexTreeHMMState lexTreeHMMState = Deencapsulation
            .newInnerInstance(LEX_TREE_HMM_STATE_CLASS_NAME, lexTreeLinguist, hmmNode, wordSequence, smearTerm,
                smearProb, hmmState, languageProbability, insertionProbability, parentNode);
        new Expectations() {{
            //@formatter:off
            hmmState.isExitState(); result = false;
            //@formatter:on

        }};



        lexTreeHMMState.getSuccessors();
        //        Ein cache mit arcs existiert cachedArcs, hole diesen
        //        Pruefe, ob exit state, bei Senone, ob nicht emitting!! (also nicht emitting==exit)
        //        fuer emitting states, mal gucken

        //        non-emitting:
        //        getSuccessors() from SenoneHMMState
        //          -> hole aus TransitionMatrix alle Werte, die ueber einem Schwellwert liegen (-3.4028235E38F)
        //             dies gilt nur fuer eine Spalte, naemlich die des states (ist ein fixer int)
        //             mach daraus eine Liste und gib zurueck, nenne ich transitionList
        //          -> erzeuge neues array new SearchStateArc[] mit Groesse der obigen Liste
        //          -> fuelle diese Liste anhand von (logInsertionProbability / getLogProbability und hmmState) aus der transitionList, oder erzeuge neue states

    }

    @Test
    public void testOurDynamicFlatLinguistGetScore() throws Exception {
        // Hole PronunciationUnit anhand des Namens von data
        //        increment numberOfTimesUsed
        //        return getConfusionScore
    }

    @Test
    public void testOurDynamicFlatLinguistGetSuccessors() throws Exception {
        //        gucke in cachedSuccessors
        //        wenn null, dann
        //

    }

    private void prepareLexTreeHmmState() {
        //        new SenoneHMMState() not public constructor
        //        new SenoneHMM(new Unit("HH", false, 25));
        //        new Unit("HH", false, 25); not public constructor
        //        HMM is emitting

        //TODO: use mocking framework

    }
}
