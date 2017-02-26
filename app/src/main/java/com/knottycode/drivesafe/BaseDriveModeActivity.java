package com.knottycode.drivesafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.transition.Fade;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.R.attr.x;

/**
 * Created by thammaknot on 2/18/17.
 */

abstract public class BaseDriveModeActivity extends Activity {
    protected String TAG = "BaseDriveModeActivity";
    protected static final int ADAPTIVE_LOUDNESS_INTERVAL_MILLIS = 3000;
    protected static final int ADAPTIVE_LOUDNESS_COUNTDOWN_DURATION = 20000;
    protected static final int ALARM_STREAM = AudioManager.STREAM_MUSIC;

    // NORMAL, CHECKPOINT, ALARM
    protected String mode = "";
    protected static final String NORMAL_MODE = "NORMAL";
    protected static final String CHECKPOINT_MODE = "CHECKPOINT";
    protected static final String ALARM_MODE = "ALARM";

    protected long checkpointFrequencyMillis;
    protected boolean adaptiveCheckpointFrequency = true;
    protected boolean adaptiveLoudness = true;
    protected Set<String> tones;
    protected Constants.AlertMode alertMode;
    protected List<String> availableAlarmTones;

    protected MediaPlayer mediaPlayer;
    protected AudioManager audioManager;
    protected CheckpointManager checkpointManager;

    protected SpeechRecognizer recognizer;
    protected ASRListener asrListener;
    protected int initialVolume;

    protected TextToSpeech tts = null;

    private void initTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                tts.setLanguage(new Locale("th", "th"));
                tts.speak("ระนอง ระยอง ยะลา", TextToSpeech.QUEUE_ADD, null, "");
                tts.speak("ยักษ์ใหญ่ไล่ยักษ์เล็ก", TextToSpeech.QUEUE_ADD, null, "");
            }
        });
    }

    protected Handler timerHandler = new Handler();
    protected Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            updateDisplay(now);
            if (!proceedToNextStep(now)) {
                timerHandler.postDelayed(this, Constants.TIMER_INTERVAL_MILLIS);
            }
        }
    };

    protected void init() {
        // Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        loadPreferences();
        checkpointManager = CheckpointManager.getInstance(checkpointFrequencyMillis,
                adaptiveCheckpointFrequency, System.currentTimeMillis());
        checkpointFrequencyMillis = checkpointManager.getNextFrequencyMillis();
        mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        asrListener = new ASRListener(this);
        startTimer();
    }

    @Override
    public void onPause() {
        audioManager.setStreamVolume(ALARM_STREAM, initialVolume, 0);
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        initialVolume = audioManager.getStreamVolume(ALARM_STREAM);
        validateSystemLoudness();
        super.onResume();
    }

    private void startTimer() {
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        adaptiveCheckpointFrequency = prefs.getBoolean(getString(R.string.adaptive_checkpoint_frequency_key), true);
        adaptiveLoudness = prefs.getBoolean(getString(R.string.adaptive_loudness_key), true);
        checkpointFrequencyMillis =
                prefs.getInt(getString(R.string.checkpoint_frequency_key),
                        Constants.DEFAULT_CHECKPOINT_FREQUENCY_SECONDS) * 1000;
        availableAlarmTones =
                new ArrayList<String>(prefs.getStringSet(getString(R.string.alarm_tones_key), Constants.allAlarmTones));
        int alertModeCode = prefs.getInt(getString(R.string.alert_style_key),
                Constants.DEFAULT_ALERT_STYLE.getCode());
        alertMode = Constants.AlertMode.fromCode(alertModeCode);
    }

    abstract protected void updateDisplay(long now);

    abstract protected boolean proceedToNextStep(long now);

    public void onASRResultsReady(List<String> results) {}

    /**
     * Fire an intent to start the voice recognition activity.
     */
    protected void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "th-TH");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(asrListener);
        recognizer.startListening(intent);
    }

    /**
     * Checks to make sure the ALARM stream's volume is sufficiently loud.
     */
    protected void validateSystemLoudness() {
        int volume = audioManager.getStreamVolume(ALARM_STREAM);
        int maxVolume = audioManager.getStreamMaxVolume(ALARM_STREAM);
        double percent = volume * 1.0 / maxVolume;
        if (percent < 0.5) {
            audioManager.setStreamVolume(ALARM_STREAM, (int) Math.ceil(maxVolume / 2.0), 0);
        }
    }

    protected void startAlarmMode() {
        stopTimer();
        Intent intent = new Intent(this, AlarmModeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    protected void startCheckpointMode() {
        stopTimer();
        Intent intent = new Intent(this, CheckpointModeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    protected void startDriveMode() {
        stopTimer();
        Intent intent = new Intent(this, DriveModeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void goBackToMainActivity() {
        stopTimer();
        checkpointManager.invalidateSingletonInstance();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.drive_mode_exit_warning_title)
                .setMessage(R.string.drive_mode_exit_warning_text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        goBackToMainActivity();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) { }})
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
