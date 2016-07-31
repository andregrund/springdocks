package de.uni.hamburg.phoneme;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PhonemeDB implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6246046410395137574L;

    private HashMap<String, String[]> hashContent = new HashMap<>();

    private List<PhonemeContainer> arrayContent = new ArrayList<>();

    public HashMap<String, String[]> getHashContent() {
        return hashContent;
    }

    public void setHashContent(final HashMap<String, String[]> hashContent) {
        this.hashContent = hashContent;
    }

    public List<PhonemeContainer> getArrayContent() {
        return arrayContent;
    }

    public void setArrayContent(final List<PhonemeContainer> arrayContent) {
        this.arrayContent = arrayContent;
    }
}


