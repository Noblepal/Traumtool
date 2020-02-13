package com.traumtool.utils;

import android.util.Log;
import android.view.View;

import com.traumtool.interfaces.ApiService;

import java.util.concurrent.TimeUnit;

import static java.util.Locale.US;

public class AppUtils {
    public static final String BASE_URL = "http://traumtool.bplaced.net/";
    public static final String RANDOM_PIC_URL = "https://source.unsplash.com/random/?nature,water";

    public static ApiService getApiService() {
        return RetrofitClient.getClient(BASE_URL).create(ApiService.class);
    }

    public static String formatStringToTime(int raw) {
        return String.format(US, "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(raw),
                TimeUnit.MILLISECONDS.toSeconds(raw) - ((TimeUnit.MILLISECONDS.toMinutes(raw)) * 60));
    }

    public static String extractFileExtensionFromString(String data) {
        return data.substring(data.lastIndexOf("."));
    }

    public static String removeFileExtensionFromString(String data) {
        return !data.equals("")
                ? data.substring(0, data.lastIndexOf("."))
                : "";
    }

    public static void showView(View v) {
        if (v.getVisibility() == View.INVISIBLE
                || v.getVisibility() == View.GONE)
            v.setVisibility(View.VISIBLE);
    }

    public static void hideView(View v) {
        if (v.getVisibility() == View.VISIBLE)
            v.setVisibility(View.GONE);
    }

    public static void tempHideView(View v) {
        if (v.getVisibility() == View.VISIBLE)
            v.setVisibility(View.INVISIBLE);
    }


    public static double bytesToMbytes(long l) {
        return roundUp2DecimalPlaces((long) (l / 1024.0 / 1024.0));
    }

    public static long roundUp2DecimalPlaces(long x) {
        return (long) (Math.round(x * 100.0) / 100.0);
    }

    public static String capitalizeEachWord(String s) {
        String[] strArray = s.split("_");
        StringBuilder builder = new StringBuilder();
        for (String str : strArray) {
            String cap = str.substring(0, 1).toUpperCase() + str.substring(1);
            builder.append(cap).append(" ");
        }
        return builder.toString();
    }

    public static void logThis(String TAG, int type, String message) {
        switch (type) {
            case 0:
                Log.d(TAG, message);
                break;
            case 1:
                Log.e(TAG, message);
                break;
            case 2:
                Log.i(TAG, message);
                break;
            default:
                Log.v(TAG, message);
        }

    }
}
