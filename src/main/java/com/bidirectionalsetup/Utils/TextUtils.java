package com.bidirectionalsetup.Utils;

public class TextUtils {
    // ANSI escape code to reset color
    private static final String ANSI_RESET = "\u001B[0m";

    public static void printInGreen(String text){
        // ANSI escape code for green color
        String ANSI_GREEN = "\u001B[32m";
        System.out.println(ANSI_GREEN + text + ANSI_RESET);
    }

    public static void printInRed(String text){
        // ANSI escape code for red color
        String ANSI_RED = "\u001B[31m";
        System.out.println(ANSI_RED + text + ANSI_RESET);
    }

}
