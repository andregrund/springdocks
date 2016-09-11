package de.uni.hamburg.postprocessor.sphinxbased;

import de.uni.hamburg.data.PhoneData;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
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

    @Before
    public void setUp() throws Exception {
    }

    /**
     * You cannot mock a primitive type, dooh <br/>
     * But, you can instead use the wrapper and {@link Injectable}, yeah
     */
    @Test
    public void testGetScore(@Mocked HMMNode hmmNode, @Mocked WordSequence wordSequence, @Injectable Float smearTerm,
        @Injectable Float smearProb, @Mocked HMMState hmmState, @Injectable Float languageProbability,
        @Injectable Float insertionProbability, @Mocked Node parentNode, @Mocked PhoneData data) throws Exception {

        new Expectations() {{
            //@formatter:off
            data.getConfusionScore(anyString, anyInt); result = 1F;
            //@formatter:on
        }};



        final LexTreeLinguistAndre.LexTreeHMMState lexTreeHmmState = Deencapsulation
            .newInnerInstance(LEX_TREE_HMM_STATE_CLASS_NAME, lexTreeLinguist, hmmNode, wordSequence, smearTerm,
                smearProb, hmmState, languageProbability, insertionProbability, parentNode);

        final float score = lexTreeHmmState.getScore(data);

        assertThat(score, is(1F));
    }
}