package de.uni.hamburg.frontend;

import de.uni.hamburg.utils.Printer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class LocalMicrophone extends AudioInputStream {

    private static String TAG = "LocalMicrophone";

    private static TargetDataLine line;

    /**
     * used internally
     */
    @Override
    public AudioFormat getFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        return format;
    }

    /**
     * creates a new audio data line with 16000 kHz sample size, 1 channel, signed, little endian
     */
    public LocalMicrophone() {
        super(null, new AudioFormat(16000, 16, 1, true, false), 0);
        final Printer printer = new Printer();
        final boolean verbose = false;
        AudioFormat format = getFormat();

        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
        line = null;
        try {
            line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        } catch (LineUnavailableException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            line.open(format);
        } catch (LineUnavailableException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        printer.printRedWithTime(TAG, "SPEAK!", verbose);
        line.start();
        printer.printWithTime(TAG, "dataline started", verbose);

    }

    @Override
    public void close() {
        new Printer().printWithTime(TAG, "dataline closed", false);
    }

    @Override
    public int read(byte[] buf, int off, int len) {
        if (line.isOpen()) {
            return line.read(buf, off, len);
        }
        return -1;
    }
}
