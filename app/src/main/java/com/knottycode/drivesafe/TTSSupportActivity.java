package com.knottycode.drivesafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class TTSSupportActivity extends AppCompatActivity {

    TextToSpeech tts;
    private static final int REQUEST_CODE_OPEN_TTS_PLAYS_STORE = 1;
    private static final int REQUEST_CODE_OPEN_TTS_PLAYS_STORE_URL = 2;
    private static final int REQUEST_CODE_OPEN_TTS_SETTINGS = 3;

    private static final String TAG = "TTSSupportActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (tts.isLanguageAvailable(Constants.THAI_LOCALE) < 0) {
                    setContentView(R.layout.activity_tts_support);
                } else {
                    // Get the shared preferences
                    SharedPreferences preferences =
                            getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);

                    // Set onboarding_complete to true
                    preferences.edit()
                            .putBoolean(getString(R.string.onboarding_complete_key), true).apply();
                    goToMainActivity();
                }
            }
        });
        super.onResume();
    }

    public void openTTSPlayStore(View view) {
        final String appPackageName = Constants.TTS_PACKAGE_NAME;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(Constants.PLAY_STORE_PREFIX + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(Constants.PLAY_STORE_URL_PREFIX + appPackageName)));
        }
    }

    public void openTTSSettings(View view) {
        Intent intent = new Intent();
        intent.setAction("com.android.settings.TTS_SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void goToMainActivity() {
        // Launch the main Activity, called MainActivity.
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);

        // Close this Activity.
        finish();
    }
/*
    final String appPackageName = Constants.TTS_PACKAGE_NAME; // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
    */
}