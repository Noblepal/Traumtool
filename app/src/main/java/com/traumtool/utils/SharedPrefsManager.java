package com.traumtool.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    private static final String SHARED_PREFS_NAME = "kilimo_link";
    private static SharedPrefsManager mInstance;
    private SharedPreferences sharedPreferences;
    //private Context mCtx;

    private SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefsManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefsManager(context);
        }
        return mInstance;
    }

    public void setOfflineMode(boolean offline) {
        sharedPreferences.edit().putBoolean("isOffline", offline).commit();
    }

    public boolean getIsOffline() {
        return sharedPreferences.getBoolean("isOffline", false);
    }
}
