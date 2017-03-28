package com.knottycode.drivesafe;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

import static com.knottycode.drivesafe.BaseDriveModeActivity.ALARM_STREAM;

public class SplashActivity extends AppCompatActivity {

    TextToSpeech tts;
    private boolean ttsSupported = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                ttsSupported = tts.isLanguageAvailable(Constants.THAI_LOCALE) >= 0;
            }
        });

        playSplashMusic();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToTTSSupportActivity() {
        Intent intent = new Intent(this, TTSSupportActivity.class);
        startActivity(intent);
        finish();
    }

    private void playSplashMusic() {
        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            AssetFileDescriptor afd =
                    getAssets().openFd(Constants.BACKGROUND_MUSTIC_PATH_PREFIX
                            + "/" + Constants.SPLASH_MUSIC_FILENAME);
            mediaPlayer.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(), afd.getLength());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                if (ttsSupported) {
                    startMainActivity();
                } else {
                    goToTTSSupportActivity();
                }
            }
        });

        try {
            mediaPlayer.setAudioStreamType(ALARM_STREAM);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}