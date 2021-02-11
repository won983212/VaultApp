package com.won983212.vaultapp.util;

import android.util.Log;

@SuppressWarnings("unused")
public class Logger {
    private static long start = 0;
    private static final String TAG = "VaultApp";

    private static String objStr(Object obj) {
        return obj == null ? "null" : obj.toString();
    }

    public static void i(Object obj) {
        Log.i(TAG, objStr(obj));
    }

    public static void d(Object obj) {
        Log.d(TAG, objStr(obj));
    }

    public static void e(Object obj) {
        Log.e(TAG, objStr(obj), new Exception());
    }

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
    }
}
