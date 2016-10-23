package de.uni.hamburg.postprocessor.sphinxbased;

import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import org.junit.Ignore;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.*;

@Ignore
public class LexTreeUnitStateTest {

    private static final Logger LOGGER = Logger.getLogger(LexTreeUnitStateTest.class.getName());

    private LexTreeLinguist lexTreeLinguist = new LexTreeLinguist();

    @Test
    public void getSuccessors() throws Exception {

        final SearchGraph searchGraph = lexTreeLinguist.getSearchGraph();
        final SearchState initialState = searchGraph.getInitialState();
        final String prettyString = initialState.toPrettyString();
        LOGGER.info(prettyString);
    }

    @Test
    public void getInsertionProbability() throws Exception {

    }

    @Test
    public void getLanguageProbability() throws Exception {

    }

    @Test
    public void getOrder() throws Exception {

    }

}