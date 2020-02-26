package com.traumtool.utils;

import android.util.Log;
import android.view.View;

import com.traumtool.interfaces.ApiService;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.util.Locale.US;

public class AppUtils {
    public static final String BASE_URL = "http://traumtool.bplaced.net/";
    public static final String RANDOM_PIC_URL = "https://source.unsplash.com/random/?nature,water";
    private static final String TAG = "AppUtils";

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
        if (!data.equals("")) {
            if (!data.contains("."))
                return data;
            else
                return data.substring(0, data.lastIndexOf("."));
        } else {
            return "";
        }
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

    /*This method will split a string in two ways
     * First: Using uppercase letters to determine where split the string
     * e.g ThisIsMyString will be {This, Is, My, String}
     * Second part will split using underscore '_'
     * e.g ThisIsMyString_AnotherPart will be {This, Is, My, String}, {Another, Part}
     */

    public static String[] stringSplitter(String rawString) {
        try { //To prevent ArrayOutPfBoundsException
            String[] bookAndAuthor = {"", ""};

            //Split using underscore
            String[] mainArray = rawString.split("_");
            Log.e(TAG, "stringSplitter: " + Arrays.toString(mainArray));//This action should return exactly two array items

            String bookName = mainArray[0];
            String authorName = mainArray[1];

            String[] bookArray = removeFirstBlankElement(bookName.split("(?=\\p{Upper})"));
            Log.e(TAG, "stringSplitter: " + Arrays.toString(bookArray));
            StringBuilder bookBuilder = new StringBuilder();
            for (String strBook : bookArray) {
                String cap = strBook.substring(0, 1).toUpperCase() + strBook.substring(1);
                bookBuilder.append(cap).append(" ");
            }

            String[] authorArray = removeFirstBlankElement(authorName.split("(?=\\p{Upper})"));
            Log.e(TAG, "stringSplitter: " + Arrays.toString(authorArray));
            StringBuilder authorBuilder = new StringBuilder();
            for (String strAuthor : authorArray) {
                String cap = strAuthor.substring(0, 1).toUpperCase() + strAuthor.substring(1);
                authorBuilder.append(cap).append(" ");
            }

            Log.e(TAG, "stringSplitter: Book Name: " + bookBuilder.toString());
            Log.e(TAG, "stringSplitter: Author Name: " + authorBuilder.toString());

            bookAndAuthor[0] = bookBuilder.toString();
            bookAndAuthor[1] = authorBuilder.toString();

            return bookAndAuthor;
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{rawString, rawString};
        }
    }

    private static String[] removeFirstBlankElement(String[] originalArray) {
        String[] newArray = new String[originalArray.length - 1];
        for (int c = 0, k = 0; c < originalArray.length; c++) {
            if (c == 0)//Remove array item at index 0
                continue;
            newArray[k++] = originalArray[c];
        }
        return newArray;
    }
}
