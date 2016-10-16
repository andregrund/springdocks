package de.uni.hamburg.postprocessor.sphinxbased.lextree;

import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;

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
