package de.uni.hamburg.postprocessor.sphinxbased;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Senone;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMM;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.SenoneHMMState;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.util.LogMath;
import mockit.Mock;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public class LexTreeWithUnitTest {

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

    @Mocked
    private HMMState hmmState;

    private SenoneHMMState lexTreeHmmState;


    @Test
    public void testLexTreeGetScore() throws Exception {
        // Aufbau HMMState
        //        HMMState
        //          -> HMM
        //              -> Unit (HH(DH,OW)), BaseUnit (HH)
        //          -> isEmitting
        //        ==> getScore(Data data) gibt cached score von Senone zurueck
    }

    @Test
    public void testLexTreeGetSuccessors(@Mocked Node parentNode) throws Exception {
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
