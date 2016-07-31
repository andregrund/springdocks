package de.uni.hamburg.phoneme;

public class PhonemePair {

    private String phoneme1;

    private String phoneme2;

    public PhonemePair(String phoneme1, String phoneme2) {
        super();
        this.phoneme1 = phoneme1;
        this.phoneme2 = phoneme2;

    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (o == this)
            return true;

        if (!o.getClass().equals(getClass()))
            return false;

        PhonemePair that = (PhonemePair) o;

        return this.phoneme1.equals(that.phoneme1) && this.phoneme2.equals(that.phoneme2);
    }
}
