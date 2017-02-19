package com.knottycode.drivesafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenStateReceiver extends BroadcastReceiver {
    private static final String TAG = "DriveSafe: ScreenStateReceiver";
    public static boolean screenOn = true;

    private static ScreenStateReceiver instance;

    public static ScreenStateReceiver getInstance() {
        if (instance == null) {
            instance = new ScreenStateReceiver();
        }
        return instance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        Log.d(TAG, "onRecieved!");
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.d(TAG, "SCREEN OFF");
            screenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Log.d(TAG, "SCREEN ON");
            screenOn = true;
        }
        */
    }

}