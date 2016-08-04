package de.uni.hamburg.postprocessor;

import de.uni.hamburg.data.LevenshteinResult;
import de.uni.hamburg.data.Result;
import de.uni.hamburg.phoneme.PhonemeContainer;
import de.uni.hamburg.phoneme.PhonemeCreator;
import de.uni.hamburg.postprocessor.levenshteinbased.Levenshtein;
import de.uni.hamburg.recognizer.RawGoogleRecognizer;
import de.uni.hamburg.recognizer.StandardRecognizer;
import de.uni.hamburg.utils.Printer;

import javax.sound.sampled.AudioInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DOCKS is a framework for post-processing results of Cloud-based speech
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * Contact:
 * 7twiefel@informatik.uni-hamburg.de
 * <br/>
 * Sentencelist heuristic. Used to match a given result containing n-best list against a list of sentences
 *
 * @author 7twiefel
 */

public class SentencelistPostProcessor implements StandardRecognizer {

    private String TAG = "LevenshteinRecognizer";

    private RawGoogleRecognizer googleRecognizer;

    private PhonemeCreator pc;

    private List<PhonemeContainer> phonemesGrammar;

    private Levenshtein ls;

    private int numberOfResults;

    private int referenceRecognizer;

    private String name = "LevenshteinRecognizer";

    /**
     * Creates a new Sentencelist postprocessor
     *
     * @param sentenceFile        path to list of sentences
     * @param numberOfResults     number of results to be returned (1 is fastest)
     * @param referenceRecognizer recognizer the result is postprocessed from
     * @param name                of the recognizer
     */
    public SentencelistPostProcessor(String sentenceFile, int numberOfResults, int referenceRecognizer, String name,
        String key) {
        this(sentenceFile, numberOfResults, key);
        this.referenceRecognizer = referenceRecognizer;
        this.name = name;
    }

    public SentencelistPostProcessor(String sentenceFile, int numberOfResults, String key) {
        googleRecognizer = new RawGoogleRecognizer(key);
        Printer.printWithTime(TAG, "loading phoneme database", );
        pc = new PhonemeCreator(sentenceFile);
        Printer.printWithTime(TAG, "getting phonemes for speech result", );
        ls = new Levenshtein();
        phonemesGrammar = pc.pdb.getArrayContent();
        this.numberOfResults = numberOfResults;
        referenceRecognizer = -1;
        Printer.printWithTime(TAG, "SentencelistPostProcessor created", );
    }

    /**
     * postprocess a result given by e.g. Google ASR
     *
     * @param result the result
     */
    @Override
    public Result recognizeFromResult(Result result) {
        //get phonemes for r
        List<PhonemeContainer> phonemesSpeech = pc.getPhonemes(result);

        if (phonemesSpeech != null) {
            Printer.printWithTime(TAG, "calculating levenshtein distances", );

            int minDist = 10000;

            int tempResult = -1;

            Printer.printWithTime(TAG, "phonemesGrammar.size: " + phonemesGrammar.size(), );

            //if one tempResult is preferred
            if (numberOfResults == 1) {
                //calculate Levenshtein distance for n-best list vs sentence list
                //take the minimal distance
                for (int i = 0; i < phonemesSpeech.size(); i++) {
                    for (int j = 0; j < phonemesGrammar.size(); j++) {
                        int diff = ls.diff(phonemesSpeech.get(i).getPhonemes(), phonemesGrammar.get(j).getPhonemes());
                        if (diff <= minDist) {
                            if (diff < minDist) {
                                minDist = diff;
                                tempResult = j;

                            }
                        }

                    }
                }

                Printer.printWithTime(TAG, "tempResult is : " + tempResult, );
                //return sentence with the minimal distance
                //phonemesGrammar.get(tempResult).print();
                result = new Result();
                result.addResult(phonemesGrammar.get(tempResult).getResult());

            } else {
                //do the same if more results are preferred
                List<LevenshteinResult> resultList = new ArrayList<LevenshteinResult>();
                for (int i = 0; i < phonemesSpeech.size(); i++) {
                    for (int j = 0; j < phonemesGrammar.size(); j++) {
                        int diff = ls.diff(phonemesSpeech.get(i).getPhonemes(), phonemesGrammar.get(j).getPhonemes());
                        resultList.add(new LevenshteinResult(diff, j, i));
                    }
                }

                //sort the list of results by smallest distance
                Collections.sort(resultList);
                result = new Result();

                for (int i = 0; (i < numberOfResults) && (i < resultList.size()); i++) {
                    result.addResult(phonemesGrammar.get(resultList.get(i).getId()).getResult());
                }

            }

            Printer.printWithTimeF(TAG, "levenshtein distances calculated");

        }
        return result;
    }

    /**
     * calculate distances of an input vs an array of strings
     *
     * @param input input sentence
     * @param array of reference sentences
     * @return array of distances to the reference sentences
     */
    public double[] calculateAgainstArray(String input, String[] array) {
        double[] res = new double[array.length];
        Result r = new Result();
        r.addResult(input);
        for (String s : array)
            r.addResult(s);
        //get phonemes
        List<PhonemeContainer> phonemesSpeech = pc.getPhonemes(r);
        //calculate distances
        for (int i = 1; i < phonemesSpeech.size(); i++) {
            int diff = ls.diff(phonemesSpeech.get(i).getPhonemes(), phonemesSpeech.get(0).getPhonemes());
            res[i - 1] = diff;

        }
        return res;

    }

    /**
     * recognize from audio file (16kHz, 1 channel, signed, little endian)
     */
    @Override
    public Result recognizeFromFile(final String fileName) {
        Result r = googleRecognizer.recognizeFromFile(fileName);
        return recognizeFromResult(r);
    }

    /**
     * recognize from LocalMicrophone or SocketMicrophone directly (using Google ASR)
     *
     * @param ai LocalMicrophone or SocketMicrophone
     * @return
     */
    public Result recognize(AudioInputStream ai) {

        Result r = googleRecognizer.recognize(ai);
        return recognizeFromResult(r);

    }

    @Override
    public int getReferenceRecognizer() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }
}
