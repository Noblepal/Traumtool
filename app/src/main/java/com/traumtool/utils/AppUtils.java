package com.traumtool.utils;

import android.view.View;

import com.traumtool.interfaces.ApiService;

import java.util.concurrent.TimeUnit;

import static java.util.Locale.US;

public class AppUtils {
    public static final String BASE_URL = "http://traumtool.bplaced.net/";

    public static ApiService getApiService() {
        return RetrofitClient.getClient(BASE_URL).create(ApiService.class);
    }

    public static ApiService getApiDownloadService() {
        return RetrofitClient.getClientDownload(BASE_URL).create(ApiService.class);
    }

    public static String formatStringToTime(int raw) {
        return String.format(US, "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(raw),
                TimeUnit.MILLISECONDS.toSeconds(raw) - ((TimeUnit.MILLISECONDS.toMinutes(raw)) * 60));
    }

    public static String extractFileExtensionFromString(String data) {
        return data.substring(data.lastIndexOf("."));
    }

    public static String removeFileExtensionFromString(String data) {
        return data.substring(0, data.lastIndexOf("."));
    }

    public static void showView(View v) {
        v.setVisibility(View.GONE);
    }

    public static void hideView(View v) {
        v.setVisibility(View.GONE);
    }


}
