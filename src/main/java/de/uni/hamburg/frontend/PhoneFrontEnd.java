package de.uni.hamburg.frontend;

import de.uni.hamburg.data.PhoneData;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.FrontEnd;

import java.util.LinkedList;
import java.util.List;

public class PhoneFrontEnd extends FrontEnd {

    private List<Data> phones;

    private int substitutionMethod;

    public PhoneFrontEnd() {
        super();
        phones = new LinkedList<>();
    }

    /**
     * sets the substitution method
     *
     * @param substitutionMethod (see PhonemeSubstitution)
     */
    public void setSubstitutionMethod(int substitutionMethod) {
        this.substitutionMethod = substitutionMethod;
    }

    /**
     * adds the phoneme sequence to the input
     *
     * @param phonemes phoneme sequence
     */
    public void addPhonemes(final String[] phonemes) {
        phones.add(new DataStartSignal(0));
        phones.add(new PhoneData("SIL", substitutionMethod));
        for (final String phoneme : phonemes) {
            phones.add(new PhoneData(phoneme, substitutionMethod));
            phones.add(new PhoneData(phoneme, substitutionMethod));
        }
        phones.add(new PhoneData("SIL", substitutionMethod));
        phones.add(new PhoneData("SIL", substitutionMethod));
        phones.add(new PhoneData("SIL", substitutionMethod));
        phones.add(new DataEndSignal(100));
    }

    /**
     * used internally
     */
    public void setDataSource(DataProcessor dataSource) {
    }

    @Override
    public Data getData() throws DataProcessingException {
        return phones.isEmpty() ? null : phones.remove(0);
    }
}
