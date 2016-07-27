package de.uni.hamburg.recognizer;

import de.uni.hamburg.data.Result;

public interface StandardRecognizer {

    Result recognizeFromResult(Result r);
    Result recognizeFromFile(String fileName);
    int getReferenceRecognizer();
    String getName();

}
