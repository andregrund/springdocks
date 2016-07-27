package de.uni.hamburg.recognizer;

import de.uni.hamburg.data.Result;
import de.uni.hamburg.utils.Printer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

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
 * Recognizer used to connect to Google ASR
 *
 * @author Johannes Twiefel
 */

public class RawGoogleRecognizer implements StandardRecognizer {

    private static final String TAG = "BaseRecognizer";

    private String name = "Google";

    private String key;

    @Override
    public Result recognizeFromResult(final Result r) {
        return null;
    }

    @Override
    public Result recognizeFromFile(final String fileName) {
        //open connection to google
        HttpURLConnection con = getConnection();

        //get stream from connection
        DataOutputStream stream = getStream(con);

        File file = new File(fileName);

        FLACFileWriter ffw = new FLACFileWriter();

        try {
            //convert whole file to FLAC
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            ffw.write(inputStream, FLACFileWriter.FLAC, boas);

            //write FLAC to stream
            stream.write(boas.toByteArray());

        } catch (IOException | UnsupportedAudioFileException e1) {
            e1.printStackTrace();
        }
        //get result
        Result res = getResult(con);

        //flush and close stream
        try {
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Printer.printWithTime(TAG, "closing");

        try {
            stream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //disconnect from google
        con.disconnect();
        Printer.printWithTime(TAG, "Done");
        // res.print();
        return res;
    }

    @Override
    public int getReferenceRecognizer() {
        return -1;
    }

    @Override
    public String getName() {
        return name;
    }

    //get result from an open connection to Google
    private Result getResult(HttpURLConnection connection) {
        BufferedReader in = null;
        Printer.printWithTime(TAG, "receiving inputstream");
        try {
            //get result stream from connection
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String decodedString = "";
        String resultJSON = null;
        Printer.printWithTime(TAG, "decoding string");
        try {
            //get JSON result
            while ((decodedString = in.readLine()) != null) {
                Printer.printWithTime(TAG, decodedString);
                resultJSON = decodedString;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (resultJSON == null) {
            Printer.printWithTime(TAG, "no JSON result");
            return null;
        }

        System.out.println(resultJSON);
        Result result = new Result();
        String utterance;
        String temp = resultJSON.substring(resultJSON.indexOf("confidence") + 13);
        if (resultJSON.equals("{\"result\":[]}"))
            return null;
        float confidence = 0;
        try {
            //check if there is no result (means no confidence)
            confidence = Float.parseFloat(temp.substring(0, temp.indexOf("}")));
        } catch (Exception e) {
            confidence = 0;
        }
        result.setConfidence(confidence);

        //parse JSON utterances
        while (resultJSON.contains("transcript")) {

            resultJSON = resultJSON.substring(resultJSON.indexOf("transcript") + 13);

            //clean from special chars
            utterance = resultJSON.substring(0, resultJSON.indexOf("\""));
            utterance = utterance.replace("@", "");

            utterance = utterance.replaceAll("[^a-zA-Z 0-9]", "");
            utterance = utterance.replaceAll(" +", " ");

            if (!utterance.equals(""))
                if (utterance.charAt(0) == ' ')
                    utterance = utterance.substring(1);

            //add to resultT
            if (!utterance.equals(""))
                result.addResult(utterance);

        }

        return result;

    }

    private static AudioFormat getAudioFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

}
