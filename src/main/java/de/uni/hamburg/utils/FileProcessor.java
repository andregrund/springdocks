package de.uni.hamburg.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileProcessor {

    private String filename;

    public FileProcessor(final String filename, final FileLoop fileloop) {
        super();
        this.filename = filename;
        try {
            FileInputStream fstream = new FileInputStream(filename);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            fileloop.start();
            while ((strLine = br.readLine()) != null) {
                fileloop.process(strLine);
            }
            br.close();
            fileloop.end();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
