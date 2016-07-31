package de.uni.hamburg.utils;

/**
 * utility class
 * @author 7twiefel
 *
 */
public interface FileLoop {

    void start();
    void process(String line);
    void end();
}
