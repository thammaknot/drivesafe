package com.knottycode.drivesafe;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DriveModeActivity extends BaseDriveModeActivity {
    TextView checkpointCountdownTimer;

    /** Time when the user last checks in at a checkpoint. */
    private long lastCheckpointTime;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private long lastTipSpokenTime = -1;
    private List<String> tipList = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = NORMAL_MODE;
        TAG = "DriveSafe: DriveModeActivity";
        lastCheckpointTime = System.currentTimeMillis();
        init();

        setContentView(R.layout.activity_drive_mode);

        RelativeLayout wholeScreenLayout = (RelativeLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return DriveModeActivity.this.onTouch(v, me);
            }
        });

        checkpointCountdownTimer = (TextView) findViewById(R.id.checkpointCountdownTimer);
        if (!checkpointManager.getVolumeAdjusted()) {
            validateSystemLoudness();
            checkpointManager.setVolumeAdjusted(true);
        }
        TextView touchScreenForQuestionTextView = (TextView) findViewById(R.id.touchScreenForQuestion);
        Animation fadeInOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_out);
        touchScreenForQuestionTextView.startAnimation(fadeInOutAnimation);
        initializeTTS();
    }

    @Override
    public void onPause() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
            tts.shutdown();
        }
        super.onPause();
    }

    @Override
    protected void updateDisplay(long now) {
        long millis = now - lastCheckpointTime;
        millis = checkpointFrequencyMillis - millis;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        checkpointCountdownTimer.setText(String.format("%d:%02d", minutes, seconds));

        long timeSinceLastTip = now - lastTipSpokenTime;
        if (ttsReady
                && getTimeRemainingInMillis() > Constants.MIN_TIME_TO_SPEAK_TIP_MILLIS &&
                (lastTipSpokenTime == -1 || timeSinceLastTip > Constants.TIP_FREQUENCY_MILLIS)) {
            lastTipSpokenTime = now;
            speakTip();
        }
    }

    @Override
    protected boolean proceedToNextStep(long now) {
        long millis = now - lastCheckpointTime;

        if (millis >= checkpointFrequencyMillis) {
            // startCheckpointMode();
            startQuestionAnswerMode();
            return true;
        }
        return false;
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    tts.setLanguage(Constants.THAI_LOCALE);
                    Set<Voice> voices = tts.getVoices();
                    for (Voice v : voices) {
                        if (v.isNetworkConnectionRequired() && v.getQuality() >= Voice.QUALITY_HIGH &&
                                v.getLocale().equals(Constants.THAI_LOCALE)) {
                            tts.setVoice(v);
                        }
                    }
                    tts.setSpeechRate(0.9f);
                    ttsReady = true;
                }
            });
    }

    private void loadTips() {
        InputStream is = null;
        AssetManager am = getAssets();
        try {
            is = am.open(Constants.TIP_FILE_PATH);
        } catch (IOException io) {
            Toast.makeText(this, "Unable to load tips", Toast.LENGTH_SHORT).show();
            io.printStackTrace();
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        tipList = new ArrayList<>();
        try {
            while ((line = r.readLine()) != null) {
                tipList.add(line);
            }
        } catch (IOException io) {
            Log.e(TAG, "Exception while reading question file.");
            io.printStackTrace();
        }
    }

    private void speakTip() {
        if (tipList == null) {
            loadTips();
        }
        int index = new Random().nextInt(tipList.size());
        String phrase = tipList.get(index);
        tts.speak(phrase, TextToSpeech.QUEUE_ADD, null, Constants.TIP_UTT_ID);
    }

    private long getTimeRemainingInMillis() {
        return checkpointFrequencyMillis - (System.currentTimeMillis() - lastCheckpointTime);
    }

    private boolean onTouch(View v, MotionEvent me) {
        if (me.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }
        long responseTimeMillis = System.currentTimeMillis() - lastCheckpointTime;
        checkpointManager.addResponseTime(responseTimeMillis);
        startQuestionAnswerMode();
        return true;
    }

    public void onClose(View v) {
        onBackPressed();
    }
}
