package com.fiuu.toppayment;

import android.util.Log;

/** Single entry point for diagnostic logging: keeps the existing Logcat output and feeds the in-app developer log panel. */
public final class DevLog {

    private DevLog() {}

    public static void d(String tag, String message) {
        Log.d(tag, message);
        DevLogBuffer.getInstance().append(tag, message);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
        DevLogBuffer.getInstance().append(tag, message);
    }
}
