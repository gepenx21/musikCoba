package com.appodeals.musikcoba;


import android.app.Activity;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.AppodealRequestCallbacks;

import java.util.Random;

public class Config {

    public static String URL = "https://ia801401.us.archive.org/0/items/shankara_202012/";
    public static final String JSON_ID = "dashuciha.json";
    public static boolean isPlaying = false;

    private static int mCount = 0;
    private static int counter = 3;

//    private final Activity activity;

//    public Config(Activity activity) {
//        this.activity = activity;
//    }

    static void randomNum() {
        int number = 7;
        counter = new Random().nextInt(number);
    }

    public void showIntersititial(Activity activity) {
        if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
            Appodeal.show(activity, Appodeal.INTERSTITIAL);
        }
    }

    
}