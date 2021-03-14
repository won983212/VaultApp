package com.won983212.vaultapp.util;

import android.util.Log;

public class Logger {
    private static final String TAG = "VaultApp";

    private static String objStr(Object obj) {
        return obj == null ? "null" : obj.toString();
    }

    public static void i(Object obj) {
        Log.i(TAG, objStr(obj));
    }

    public static void e(Object obj) {
        Log.e(TAG, objStr(obj), new Exception());
    }

    // time measuring
    /*private static long start = 0;
    public static void beginMeasure(String label) {
        if (label != null) d(label);
        start = System.nanoTime();
    }

    public static void beginMeasure() {
        beginMeasure(null);
    }

    public static void printMeasure(String label) {
        long t = System.nanoTime();
        d(label + ": " + (t - start) / 1000000.0 + "ms");
        start = t;
    }*/
}
