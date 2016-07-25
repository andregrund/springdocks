package de.uni.hamburg.example;

import Frontend.LocalMicrophone;
import Frontend.VoiceActivityDetector;
import PostProcessor.SentencelistPostProcessor;
import PostProcessor.SphinxBasedPostProcessor;
import PostProcessor.WordlistPostProcessor;
import Recognizer.RawGoogleRecognizer;
import Recognizer.SphinxRecognizer;
import Utils.ExampleChooser;
import Utils.Printer;
import de.uni.hamburg.data.Result;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
 * Class showing different examples of usage
 *
 * @author 7twiefel
 * @author 5grund
 */
class Example {

    // utility to test recognizers and postprocessors on an example audio file
    private static void testFile(String filename, String sentence, RawGoogleRecognizer rawGoogleRecognizer,
        SentencelistPoprivate static final long serialVersionUID=1650789290776731090L;

        private ArrayList<String>resultList=new ArrayList<String>();

    private float confidence;

    private String hypPhoneme;

    private String refPhoneme;

    /**
     * @return phonemes of hypothesis
     */
    public String getHypPhoneme() {
        return hypPhoneme;
    }

    /**
     * @param hypPhoneme phonemes of hypothesis
     */
    public void setHypPhoneme(String hypPhoneme) {
        this.hypPhoneme = hypPhoneme;
    }

    /**
     * @return phonemes of reference
     */
    public String getRefPhoneme() {
        return refPhoneme;
    }

    /**
     * @return phonemes of reference
     */
    public void setRefPhoneme(String refPhoneme) {
        this.refPhoneme = refPhoneme;
    }

    /**
     * @return n-best list
     */
    public List<String> getResultList() {
        return resultList;
    }

    /**
     * @param resultList sets n-best list
     */
    public void setResultList(List<String> resultList) {
        this.resultList = resultList;
    }

    /**
     * @param s adds result string to n-best list
     */
    public void addResult(String s) {
        resultList.add(s);
    }

    /**
     * @return best result of n-best list
     */
    public String getBestResult() {
        return resultList.get(0);
    }

    /**
     * @param f sets confidence for best result
     */
    public void setConfidence(float f) {
        confidence = f;
    }

    /**
     * @return n-best list as array
     */
    public String[] getResult() {
        String[] result = new String[resultList.size()];
        resultList.toArray(result);
        return result;
    }

    /**
     * prints out n-best list
     */
    public void print() {
        System.out.println("");
        System.out.println("= = = = = = = = =");
        System.out.println("Results");
        System.out.println("");
        System.out.println("Confidence: " + confidence);
        System.out.println("");
        System.out.println("N-Best List: ");
        System.out.println("");
        for (String s : resultList) {
            System.out.println(s);
        }
        System.out.println("= = = = = = = = =");
        System.out.println("");
    }

    /**
     * writes n-best list to a words.txt
     */
    public void writeToFile() {
        try {
            // Create file
            FileWriter fstream = new FileWriter("words.txt");
            BufferedWriter out = new BufferedWriter(fstream);
            for (String s : resultList) {
                out.write(s + "\n");
            }

            // Close the output stream
            out.close();
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * serializes result to a file
     *
     * @param file filename
     */
    public void save(String file) {
        OutputStream fos = null;

        try {

            fos = new FileOutputStream(file);
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(this);

        } catch (IOException e) {
            System.err.println(e);
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * loads result from file
     *
     * @param file filename
     * @return loaded result
     */
    public static Result load(String file) {
        InputStream fis = null;
        Result r = null;

        try {

            fis = new FileInputStream(file);
            ObjectInputStream o = new ObjectInputStream(fis);
            r = (Result) o.readObject();
        } catch (IOException e) {
            System.err.println(e);
        } catch (ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }

        return r;
    }

    stProcessor sentencelist, SphinxRecognizer

    sphinxNGram,

    SphinxRecognizer sphinxSentences, SphinxBasedPostProcessor sphinxBasedPostProcessorBigram, SphinxBasedPostProcessor sphinxBasedPostProcessorUnigram, SphinxBasedPostProcessor sphinxBasedPostProcessorSentences, WordlistPostProcessor

    wordlist)

    {

        // clean the sentences from special chars
        sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
        sentence = sentence.replaceAll(" +", " ");
        if (!sentence.equals(""))
            if (sentence.charAt(0) == ' ')
                sentence = sentence.substring(1);

        Printer.printColor(Printer.ANSI_CYAN, sentence);

        // containers for results
        String hypRawGoogle = "";
        String hypSentenceList = "";
        String hypWordList = "";
        String hypSphinxPostProcessorUnigram = "";
        String hypSphinxPostProcessorBigram = "";
        String hypSphinxPostProcessorSentences = "";

        // recognize from google
        Result r = null;
        r = rawGoogleRecognizer.recognizeFromFile(filename);

        if (r != null) {
            // print out result
            hypRawGoogle = r.getBestResult();
            System.out.println("Raw Google: " + hypRawGoogle);

            // recognize from google result
            r = sentencelist.recognizeFromResult(r);
            if (r != null)
                hypSentenceList = r.getBestResult();
            System.out.println("Google+Sentencelist: " + hypSentenceList);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);

            r = wordlist.recognizeFromResult(r);
            if (r != null)
                hypWordList = r.getBestResult();
            System.out.println("Google+Wordlist: " + hypWordList);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorBigram.recognizeFromResult(r);
            if (r != null)
                hypSphinxPostProcessorBigram = r.getBestResult();
            System.out.println("Google+Sphinx N-Gram: " + hypSphinxPostProcessorBigram);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorUnigram.recognizeFromResult(r);
            if (r != null)
                hypSphinxPostProcessorUnigram = r.getBestResult();
            System.out.println("Google+Sphinx Unigram: " + hypSphinxPostProcessorUnigram);

            // refill result
            r = new Result();
            r.addResult(hypRawGoogle);
            // recognize from google result
            r = sphinxBasedPostProcessorSentences.recognizeFromResult(r);
            if (r != null)
                hypSphinxPostProcessorSentences = r.getBestResult();
            System.out.println("Google+Sphinx Sentences: " + hypSphinxPostProcessorSentences);

        }
        // recognize from file
        r = sphinxSentences.recognizeFromFile(filename);
        String result = "";
        if (r != null) {
            result = r.getBestResult();

        }
        System.out.println("Sphinx Sentences: " + result);

        // recognize from file
        r = sphinxNGram.recognizeFromFile(filename);
        result = "";
        if (r != null) {
            result = r.getBestResult();

        }
        System.out.println("Sphinx N-Gram: " + result);

    }

    // utility to play audio
    private static void playSound(String filename) {

        File file = new File(filename);

        try {

            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);

            AudioFormat format = inputStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);

            clip = (Clip) AudioSystem.getLine(info);

            clip.open(inputStream);
            clip.start();

        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Get a sound clip resource.
        catch (LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // interrupt till enter is pressed
    private static void waitForEnter() {
        Console c = System.console();
        if (c != null) {

            c.format("\nPress ENTER to proceed.\n");
            c.readLine();
        }
    }

    // a simple example simulation input from files
    private static void exampleSimulation(String key) {

        // Start with current sphinx configuration
        //        final Configuration configuration = new Configuration();
        //        configuration.setAcousticModelPath("");

        //        final StreamSpeechRecognizer streamSpeechRecognizer = new StreamSpeechRecognizer(configuration);
        //        streamSpeechRecognizer.

        // this is the config name. it is used as prefix for all configuration
        // file like wtm_experiment.sentences.txt

        //String configname = "wtm_experiment";
        String configname = "config/elpmaxe/elpmaxe";

        // initialize some recognizers
        System.out.println("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

        System.out.println("Starting Google+Sentencelist");
        SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(configname + ".sentences", 1, key);

        System.out.println("Starting Sphinx N-Gram");
        SphinxRecognizer sphinxNGram = new SphinxRecognizer(configname + ".ngram.xml");

        System.out.println("Starting Sphinx Sentences");
        SphinxRecognizer sphinxSentences = new SphinxRecognizer(configname + ".fsgsentences.xml");

        System.out.println("Starting Google+Sphinx N-Gram");
        final SphinxBasedPostProcessor sphinxPostProcessorBigram = new SphinxBasedPostProcessor(
            configname + ".pngram.xml", configname + ".words", 0, 0, 0);

        System.out.println("Starting Google+Sphinx Unigram");
        final SphinxBasedPostProcessor sphinxPostProcessorUnigram = new SphinxBasedPostProcessor(
            configname + ".punigram.xml", configname + ".words", 0, 0, 0);

        System.out.println("Starting Google+Sphinx Sentences");
        final SphinxBasedPostProcessor sphinxPostProcessorSentences = new SphinxBasedPostProcessor(
            configname + ".pgrammarsentences.xml", configname + ".words", 0, 0, 0);

        System.out.println("Starting Google+Wordlist");
        WordlistPostProcessor wordlist = new WordlistPostProcessor(configname + ".words", key);

        // a testfile
        String filename = "data/back_fs_1387386033021_m1.wav";
        // the reference text
        String sentence = "there is a door in the back";
        // play sound before recognition
        //		playSound(filename);
        // recognize
        testFile(filename, sentence, rawGoogle, sentencelist, sphinxNGram, sphinxSentences, sphinxPostProcessorBigram,
            sphinxPostProcessorUnigram, sphinxPostProcessorSentences, wordlist);

        waitForEnter();

        filename = "data/front_fs_1387379085134_m1.wav";
        sentence = "the door is in front of you";
        //		playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist, sphinxNGram, sphinxSentences, sphinxPostProcessorBigram,
            sphinxPostProcessorUnigram, sphinxPostProcessorSentences, wordlist);
        waitForEnter();
        filename = "data/home_fs_1387379071054_m1.wav";

        sentence = "the kitchen is at home";
        //		playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist, sphinxNGram, sphinxSentences, sphinxPostProcessorBigram,
            sphinxPostProcessorUnigram, sphinxPostProcessorSentences, wordlist);
        waitForEnter();

        filename = "data/show_fs_1387385878857_m1.wav";
        sentence = "robot show me the pen";
        //		playSound(filename);
        testFile(filename, sentence, rawGoogle, sentencelist, sphinxNGram, sphinxSentences, sphinxPostProcessorBigram,
            sphinxPostProcessorUnigram, sphinxPostProcessorSentences, wordlist);
    }

    public static void exampleLive(String key) {

        // define config name
        String configname = "config/elpmaxe/elpmaxe";

        // load google
        System.out.println("Starting Raw Google");
        RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

        // load sentencelist postprocessor
        System.out.println("Starting Google+Sentencelist");
        SentencelistPostProcessor sentencelist = new SentencelistPostProcessor(configname + ".sentences", 1, key);

        Result r;
        // load example chooser
        ExampleChooser ec = new ExampleChooser(configname + ".sentences");

        // load voice activity detection
        VoiceActivityDetector vac = new VoiceActivityDetector(new LocalMicrophone(), "LocalMicrophone");

        while (true) {
            // print random sentence
            ec.printRandomExample();

            // recognize from microphone
            r = rawGoogle.recognize(vac);

            String rawGoogleResult = "";
            String sentenceListResult = "";

            if (r != null) {
                // get google result
                rawGoogleResult = r.getBestResult();
                System.out.println("Raw Google: " + rawGoogleResult);

                // postprocess with sentencelist
                r = sentencelist.recognizeFromResult(r);

                // get result
                if (r != null)
                    sentenceListResult = r.getBestResult();
                System.out.println("Google+Sentencelist: " + sentenceListResult);

            }

        }
    }

    public static String join(String[] strings, String delimiter) {
        if (strings == null || strings.length == 0)
            return "";
        StringBuilder builder = new StringBuilder(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            builder.append(delimiter).append(strings[i]);
        }
        return builder.toString();
    }

    public static void main(String[] args) {
        // set verbose to false
        Printer.verbose = false;

        //uncomment this to create a new configuration from a batch file
        //ConfigCreator.createConfig("elpmaxe", "./batch");

        //put your Google key here
        //String key = "yourkeyhere";
        String key = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw";

        //starts the simulation example
        exampleSimulation(key);

        // starts the live recognition example
        exampleLive(key);

    }
}
