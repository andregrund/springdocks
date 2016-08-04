package de.uni.hamburg.utils;

public class Printer {

    private static long startTime;

    private int logLevel = 0;

    private static final int NONE = 0;

    private static final int INFO = 1;

    private static final int FINE = 2;

    static void printGreen(String s) {
        System.out.print(Colors.ANSI_GREEN);
        System.out.print(s);
        System.out.println(Colors.ANSI_RESET);
    }

    public void printColor(Colors color, String text, final boolean verbose) {
        if (verbose) {
            System.out.print(color);
            System.out.print(text);
            System.out.println(Colors.ANSI_RESET);
        }
    }

    public void printRed(String s) {
        System.out.print(Colors.ANSI_RED);
        System.out.print(s);
        System.out.println(Colors.ANSI_RESET);
    }

    public synchronized void printWithTime(String TAG, String s, final boolean verbose) {
        if (verbose) {
            System.out.print("[" + (System.currentTimeMillis() - startTime) + " MS]");
            System.out.println(TAG + ": " + s);
        }
    }

    public synchronized void printWithTimeI(String TAG, String s) {
        if (logLevel >= INFO) {
            System.out.print("[" + (System.currentTimeMillis() - startTime) + " MS]");
            System.out.println(TAG + ": " + s);
        }
    }

    public synchronized void printWithTimeF(String TAG, String s) {
        if (logLevel >= FINE) {

            System.out.print("[" + (System.currentTimeMillis() - startTime) + " MS]");
            System.out.println(TAG + ": " + s);
        }
    }

    public synchronized void printfWithTimeF(String TAG, String s) {
        if (logLevel >= FINE) {
            System.out.print("[" + (System.currentTimeMillis() - startTime) + " MS]");
            System.out.print(TAG + ": " + s);
        }
    }

    public synchronized void printfWithTime(String TAG, String s, final boolean verbose) {
        if (verbose) {
            System.out.print("[" + (System.currentTimeMillis() - startTime) + " MS]");
            System.out.print(TAG + ": " + s);
        }
    }

    public synchronized void printRedWithTime(String TAG, String s, final boolean verbose) {
        if (verbose) {

            System.out.print("[" + (System.currentTimeMillis() - startTime) + " MS]");
            System.out.print(TAG + ": ");
            System.err.println(s);
        }
    }

    public synchronized void printWithTimeImportant(String TAG, String s) {
        System.out.print("[" + (System.currentTimeMillis() - startTime) + " MS]");
        System.out.println(TAG + ": " + s);

    }

    public long resetExecutionTime() {
        long result = System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();
        return result;
    }

    public synchronized void printTime() {
        System.out.print("[" + (System.currentTimeMillis() - startTime) + " MS]");
    }
}
