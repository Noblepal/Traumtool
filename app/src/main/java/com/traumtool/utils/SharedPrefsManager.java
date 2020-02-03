package com.traumtool.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    private static final String SHARED_PREFS_NAME = "traumtool";
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

    public void toggleOfflineMode(boolean mode) {
        sharedPreferences.edit().putBoolean("isOffline", mode).commit();
    }

    public void setIsBackgroundAudioPlaying(boolean isAudioPlaying) {
        sharedPreferences.edit().putBoolean("isBackGroundPlayerPlaying", isAudioPlaying).apply();
    }

    public boolean isBackGroundAudioPlaying() {
        return sharedPreferences.getBoolean("isBackGroundPlayerPlaying", false);
    }

    public boolean getIsOffline() {
        return sharedPreferences.getBoolean("isOffline", false);
    }

    public void setCurrentCategory(String category) {
        sharedPreferences.edit().putString("_current_category", category).commit();
    }


    public void setAudioFileUriOrUrl(String uri_url) {
        sharedPreferences.edit().putString("uri_url", uri_url).commit();
    }

    public void setCurrentAudioName(String name) {
        sharedPreferences.edit().putString("_audio_name", name).commit();
    }

    public String getCurrentAudioName() {
        return AppUtils.removeFileExtensionFromString(sharedPreferences.getString("_audio_name", "none"));
    }

    public void setIsAudioStreaming(Boolean isStreaming) {
        sharedPreferences.edit().putBoolean("isStreaming", isStreaming).commit();
    }


    public String getCurrentCategory() {
        return sharedPreferences.getString("_current_category", "none");
    }

    public void setCurrentPosition(int position) {
        sharedPreferences.edit().putInt("current_position", position).commit();
    }

    public int getCurrnentPosition() {
        return sharedPreferences.getInt("current_position", 0);
    }

}
