package de.uni.hamburg.postprocessor.sphinxbased;

import de.uni.hamburg.data.PhoneData;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.util.LRUCache;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JMockit.class)
public class LexTreeLinguistAndreTest {

    private static final String LEX_TREE_HMM_STATE_CLASS_NAME = "LexTreeHMMState";

    @Tested
    private LexTreeLinguistAndre lexTreeLinguist;

    @Mocked
    private Node node;

    @Before
    public void setUp() throws Exception {
    }

    /**
     * You cannot mock a primitive type, dooh <br/>
     * But, you can instead use the wrapper and {@link Injectable}, yeah
     */
    @Test
    public void testGetScore(@Mocked HMMNode unitNode, @Mocked WordSequence wordSequence,
        @Injectable("1.3") float smearTerm, @Injectable("2.3") float smearProb, @Mocked HMMState hmmState,
        @Injectable("3.3") float languageProbability, @Injectable("4.3") float insertionProbability,
        @Mocked Node parentNode, @Mocked PhoneData data) throws Exception {

        new Expectations() {{
            //@formatter:off
            data.getConfusionScore(anyString, anyInt); result = 1F;
            //@formatter:on
        }};

        final LexTreeLinguistAndre.LexTreeHMMState lexTreeHmmState = Deencapsulation
            .newInnerInstance(LEX_TREE_HMM_STATE_CLASS_NAME, lexTreeLinguist, unitNode, wordSequence, smearTerm,
                smearProb, hmmState, languageProbability, insertionProbability, parentNode);

        final float score = lexTreeHmmState.getScore(data);

        assertThat(score, is(1F));
    }

    @Test
    public void testLexTreeGetSuccessorsNotNullNoCachedArcs(@Mocked UnitNode unitNode, @Mocked WordSequence wordSequence,
        @Injectable Float smearTerm, @Injectable Float smearProb, @Mocked HMMState hmmState,
        @Injectable Float languageProbability, @Injectable Float insertionProbability, @Mocked Node parentNode,
        @Injectable("true") boolean cacheEnabled,
        @Injectable LRUCache<LexTreeLinguistAndre.LexTreeState, SearchStateArc[]> arcCache) throws Exception {

        new Expectations() {{
            //@formatter:off
            arcCache.get((SearchStateArc) any); result = null;
            //@formatter:on
        }};

        final LexTreeLinguistAndre.LexTreeHMMState lexTreeHmmState = Deencapsulation
            .newInnerInstance(LEX_TREE_HMM_STATE_CLASS_NAME, lexTreeLinguist, unitNode, wordSequence, smearTerm,
                smearProb, hmmState, languageProbability, insertionProbability, parentNode);

        final SearchStateArc[] successors = lexTreeHmmState.getSuccessors();
    }
}