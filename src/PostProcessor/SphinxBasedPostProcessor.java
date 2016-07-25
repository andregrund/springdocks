/**
 * DOCKS is a framework for post-processing results of Cloud-based speech 
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact:
 * 7twiefel@informatik.uni-hamburg.de
 */
package PostProcessor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Data.Result;
import Data.TokenGraphDumper;
import Data.Visualizer;
import Frontend.PhoneFrontEnd;
import Phoneme.PhonemeContainer;
import Phoneme.PhonemeCreator;
import PostProcessor.SphinxBased.LexTreeLinguist;
import Recognizer.StandardRecognizer;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.util.LinguistDumper;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.Sausage;
import edu.cmu.sphinx.result.SausageMaker;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

/**
 * based on sphinx example
 * modified by Johannes Twiefel
 *
 */
public class SphinxBasedPostProcessor implements StandardRecognizer
{

    private ConfigurationManager cm;
    private Recognizer recognizer;

    private PhonemeCreator pc;
    private PhoneFrontEnd pfe;
    private int referenceRecognizer;

    private String name = "PhonemeNgramRecognizer";

    /**
     * creates a new sphinx based postprocessor
     * @param configName name of the config. this is used as a prefix for all xml config, languague model, sentence list word list files etc.
     * @param sentenceFile path to list of sentences
     * @param languageWeight parameter used internally, but 0 here if you don't know what you are doing
     * @param wordInsertionProbability parameter used internally, but 0 here if you don't know what you are doing
     * @param substitutionMethod parameter used internally, but 0 here if you don't know what you are doing
     */
    public SphinxBasedPostProcessor(String configName, String sentenceFile,
            float languageWeight, float wordInsertionProbability,
            int substitutionMethod)
    {
        //load config xml
        try
        {
            cm = new ConfigurationManager(new File(configName).toURI()
                .toURL());
        }
        catch (PropertyException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        referenceRecognizer = -1;

        //ACTUNG DIE GEHÖREN EIG AUF 0
        if (languageWeight != 0)
            cm.setGlobalProperty("languageWeight", languageWeight + "");
        if (wordInsertionProbability != 0)
            cm.setGlobalProperty("wordInsertionProbability",
                    wordInsertionProbability + "");
        
        System.out.println("LW: " + getLanguageWeight() + " WIP: " + getWIP());
        
        recognizer = (Recognizer) cm.lookup("recognizer");
        recognizer.allocate();
        
        //initialize frontend, recognizer and phoneme creator
        pfe = (PhoneFrontEnd) cm.lookup("frontend");
        pfe.setSubstitutionMethod(substitutionMethod);


        pc = new PhonemeCreator(sentenceFile);

    }

    /**
     * 
     * @return Language Weight
     */
    public float getLanguageWeight()
    {
        return Float.parseFloat(cm.getGlobalProperty("languageWeight"));
    }

    /**
     * 
     * @return Word Insertion Probability
     */
    public float getWIP()
    {
        return Float.parseFloat(cm.getGlobalProperty("wordInsertionProbability"));
    }

    /**
     * postprocesses the results of an ASR
     * @param r result of an ASR like Google ASR
     */
    public Result recognizeFromResult(Result r)
    {
        //get phonemes
        ArrayList<PhonemeContainer> phonemesSpeech = pc.getPhonemes(r);
        if(phonemesSpeech.size()==0)
            return null;
        //get best result
        String[] phonemes = phonemesSpeech.get(0)
            .getPhonemes();
        
//        PRINTES OUT THE PHONEMES OF THE GOOGLEHYPOTHESE
//        System.out.print(phonemes.length+" Phoneme :");
        for (int i = 0; i < phonemes.length; i++)
        {
            System.out.print(phonemes[i]+", ");
        }
//        System.out.println();

        //ad to phone frontend
        pfe.addPhonemes(phonemes);

        //start postprocessing
        r = null;
        edu.cmu.sphinx.result.Result result;
        while ((result = recognizer.recognize()) != null)
        {
            
//          LexTreeLinguist odfl = (LexTreeLinguist) cm.lookup("lexTreeLinguist");
//          LinguistDumper ld = new LinguistDumper("bla.blub",odfl);
//          ld.run();
//          System.out.println(odfl.getSearchGraph().getInitialState());
//          
//          TokenGraphDumper tgd = new TokenGraphDumper(result);
//          tgd.dumpGDL(result.getBestFinalResultNoFiller(), "graph.gdl");
//          Runtime rt = Runtime.getRuntime();
//          try {
//              try {
//                  Thread.sleep(2000);
//              } catch (InterruptedException e) {
//                  // TODO Auto-generated catch block
//                  e.printStackTrace();
//              } 
//              rt.exec("graph-easy --as=dot graph.gdl --output=graph.dot");
//              try {
//                  Thread.sleep(1000);
//              } catch (InterruptedException e) {
//                  // TODO Auto-generated catch block
//                  e.printStackTrace();
//              } 
//              rt.exec("dot -Tpng graph.dot -o graph.png");
//              try {
//                  Thread.sleep(4000);
//              } catch (InterruptedException e) {
//                  // TODO Auto-generated catch block
//                  e.printStackTrace();
//              } 
//              new Visualizer().start(); 
//          } catch (IOException e) {
//              // TODO Auto-generated catch block
//              e.printStackTrace();
//          }
//
//          Dictionary dic = (Dictionary) cm.lookup("dictionary");
//          try {
//              dic.allocate();
//          } catch (IOException e) {
//              // TODO Auto-generated catch block
//              e.printStackTrace();
//          }
//          System.out.println(dic);
//
//          Lattice lat;
//          try {
//              //lat = GDLLatticeFactory.getLattice("graph.gdl", dic);
//              lat = new Lattice(result);
//              LatticeOptimizer lo = new LatticeOptimizer(lat);
//              lo.optimize();
//              lat.dumpDot("lattice.dot","Search Graph");
//              lat.dumpAISee("lattice.gdl","Search Graph");
//
//              SausageMaker sm = new SausageMaker();
//              Sausage ss = sm.makeSausage();
//
//              ss.dumpAISee("sausage.gdl", "Sausage");
//          } catch (Exception e) {
//              // TODO Auto-generated catch block
//              e.printStackTrace();
//          }
            
//           ___________________________________________________________________
            if (r == null) r = new Result();

            String refPhoneme = "";
            for (String s : phonemes)
                refPhoneme = refPhoneme + s + " ";

            //get phoneme sequence of hypotheses
            String[] s2 = result.getBestPronunciationResult()
                .split(" ");
            Pattern p = Pattern.compile(".*\\[(.*)\\]");

            String hypPhoneme = "";
            //get phonemes for hypotheses phoneme sequence
            for (String s : s2)
            {
                Matcher m = p.matcher(s);
                if (m.find())
                {
                    String[] s3 = m.group(1)
                        .split(",");
                    for (String s4 : s3)
                        hypPhoneme = hypPhoneme + s4 + " ";
                }
            }

            //set the phoneme sequences in the result
            r.setRefPhoneme(refPhoneme);
            r.setHypPhoneme(hypPhoneme);

            //get best result
            String resultText = result.getBestFinalResultNoFiller();
//            if (resultText.equals("")) return null;
            r.addResult(resultText);

            //add rest to 10-best list
            int i = 0;
            for (Token t : result.getResultTokens())
            {

                if (i >= 9) break;
                r.addResult(t.getWordPathNoFiller());
                i++;
            }

        }
        return r;

    }

    @Override
    public Result recognizeFromFile(String fileName)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getReferenceRecognizer()
    {
        // TODO Auto-generated method stub
        return referenceRecognizer;
    }

    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return name;
    }

}
