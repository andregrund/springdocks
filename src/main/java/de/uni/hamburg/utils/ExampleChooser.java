package de.uni.hamburg.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class ExampleChooser {

    private ArrayList<String> sentences = new ArrayList<>();

    private Random gen = new Random();

    private int size = 0;

    //creates a new example chooser
    public ExampleChooser(String sentenceFile) {
        super();
        Scanner in;
        try {
            in = new Scanner(new FileReader(sentenceFile + ".txt"));

            in.useDelimiter("\n");

            String temp = "";
            while (in.hasNext()) {
                temp = in.next();
                if (temp.indexOf("\r") != -1) {
                    temp = temp.substring(0, temp.length() - 1);
                } else
                    temp = temp.substring(0, temp.length());

                temp = temp.replaceAll("[^a-zA-Z 0-9]", "");
                temp = temp.replaceAll(" +", " ");
                if (temp.charAt(0) == ' ')
                    temp = temp.substring(1);

                sentences.add(temp.toLowerCase());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        size = sentences.size();

    }

    /**
     * prints out a random sentence from the sentencelist
     *
     * @return the random sentence
     */
    public String printRandomExample() {
        final boolean verbose = false;
        final Printer printer = new Printer();
        System.out.println();
        int i = gen.nextInt(size - 1);
        System.out.println("Please read the following sentence:");
        printer.printColor(Colors.ANSI_BLUE,
            "\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0", verbose);
        System.out.println();
        System.out.println(sentences.get(i));
        System.out.println();
        printer.printColor(Colors.ANSI_BLUE,
            "\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0", verbose);
        return sentences.get(i);
    }
}
