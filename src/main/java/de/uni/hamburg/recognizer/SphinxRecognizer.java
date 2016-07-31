package de.uni.hamburg.recognizer;

import de.uni.hamburg.data.Result;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * Modified by Johannes Twiefel
 * <br/>
 * A simple HelloWorld demo showing a simple speech application built using
 * Sphinx-4. This application uses the Sphinx-4 endpointer, which automatically
 * segments incoming audio into utterances and silences.
 */
public class SphinxRecognizer implements StandardRecognizer {

    private String name = "SimpleSphinxRecognizer";

    private Recognizer recognizer;

    private Microphone microphone;

    private ConfigurationManager cm;

    private AudioFileDataSource dataSource;

    public SphinxRecognizer(String configName, String name) {
        this(configName);
        this.name = name;
    }

    public SphinxRecognizer(String configName) {
        //get config and initialize
        try {
            cm = new ConfigurationManager(new File(configName).toURI().toURL());
        } catch (PropertyException | MalformedURLException e) {
            e.printStackTrace();
        }
        recognizer = cm.lookup("recognizer");
        recognizer.allocate();
        microphone = cm.lookup("microphone");
        dataSource = cm.lookup("audioFileDataSource");
    }

    @Override
    public Result recognizeFromFile(final String fileName) {
        URL audioURL = null;
        try {
            audioURL = new File(fileName).toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // configure the audio input for the recognizer
        dataSource.setAudioFile(audioURL, null);

        // Loop until last utterance in the audio file has been decoded, in which case the recognizer will return null.
        Result r = null;
        edu.cmu.sphinx.result.Result result;
        while ((result = recognizer.recognize()) != null) {
            if (r == null)
                r = new Result();
            //get best result and add to 10-best list
            String resultText = result.getBestFinalResultNoFiller();
            if (resultText.equals(""))
                return null;

            r.addResult(resultText);

            //get rest 9 results and add to 10-best list
            int i = 0;
            for (Token t : result.getResultTokens()) {

                if (i >= 9)
                    break;
                r.addResult(t.getWordPathNoFiller());
                i++;
            }
            //
        }
        return r;
    }

    @Override
    public Result recognizeFromResult(final Result r) {
        return null;
    }

    @Override
    public int getReferenceRecognizer() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    /**
     * @return Language Weight
     */
    public float getLanguageWeight() {
        return Float.parseFloat(cm.getGlobalProperty("languageWeight"));
    }

    /**
     * @return Word Insertion Probability
     */
    public float getWIP() {
        return Float.parseFloat(cm.getGlobalProperty("wordInsertionProbability"));
    }
}
