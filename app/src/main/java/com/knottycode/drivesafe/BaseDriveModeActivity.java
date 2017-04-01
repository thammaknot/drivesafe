package com.knottycode.drivesafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    protected static final String QUESTION_ANSWER_MODE = "QA";

    protected long checkpointFrequencyMillis;
    protected Set<String> tones;
    protected List<String> availableAlarmTones;

    protected MediaPlayer mediaPlayer;
    protected AudioManager audioManager;
    protected CheckpointManager checkpointManager;

    protected SpeechRecognizer recognizer;
    protected ASRListener asrListener;
    protected int initialVolume;
    protected boolean activeASR = false;

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
                System.currentTimeMillis(), this);
        checkpointFrequencyMillis = checkpointManager.getNextFrequencyMillis();
        mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        asrListener = new ASRListener(this);
        startTimer();
    }

    @Override
    public void onPause() {
        // audioManager.setStreamVolume(ALARM_STREAM, initialVolume, 0);
        if (recognizer != null) {
            Log.d(TAG, "ASR Listener:: calling stopRecognition from onPause");
            asrListener.stopRecognition();
            recognizer.destroy();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        // initialVolume = audioManager.getStreamVolume(ALARM_STREAM);
        // validateSystemLoudness();
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
        checkpointFrequencyMillis =
                prefs.getInt(getString(R.string.checkpoint_frequency_key),
                        Constants.DEFAULT_CHECKPOINT_FREQUENCY_SECONDS) * 1000;
        availableAlarmTones =
                new ArrayList<String>(prefs.getStringSet(getString(R.string.alarm_tones_key), Constants.ALL_ALARM_TONES));
    }

    abstract protected void updateDisplay(long now);

    abstract protected boolean proceedToNextStep(long now);

    public void onASRResultsReady(List<String> results) {}

    /**
     * Fire an intent to start the voice recognition activity.
     */
    protected void startVoiceRecognitionActivity() {
        FirebaseCrash.log("Starting voice recognition");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "th-TH");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, Constants.MAX_ASR_RESULTS);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(asrListener);
        asrListener.setRecognizer(recognizer, intent);
        activeASR = true;
        asrListener.startRecognition();
    }

    /**
     * Checks to make sure the ALARM stream's volume is sufficiently loud.
     */
    protected void validateSystemLoudness() {
        int volume = audioManager.getStreamVolume(ALARM_STREAM);
        int maxVolume = audioManager.getStreamMaxVolume(ALARM_STREAM);
        if (volume < maxVolume * 4 / 5) {
            volume = maxVolume * 4 / 5;
        }
        audioManager.setStreamVolume(ALARM_STREAM, volume, 0);
    }

    protected void startAlarmMode() {
        stopTimer();
        FirebaseCrash.report(new Exception("Starting Alarm Mode"));
        Intent intent = new Intent(this, AlarmModeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    protected void startQuestionAnswerMode() {
        stopTimer();
        Intent intent = new Intent(this, QuestionAnswerActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    protected void startDriveMode() {
        stopTimer();
        Intent intent = new Intent(this, DriveModeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void goBackToMainActivity() {
        stopTimer();
        checkpointManager.invalidateSingletonInstance();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void saveStats() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mFirebaseAuth.getCurrentUser();

        if (user != null && mDatabase != null) {
            String uid = user.getUid();
            long duration = System.currentTimeMillis() - checkpointManager.getDriveModeStartTime();
            TripStats trip =
                    new TripStats(duration, checkpointManager.getScore(),
                            checkpointManager.getDriveModeStartTime());
            mDatabase.child("users").child(uid).child("stats").push().setValue(trip);
        } else {
            Toast.makeText(this, getString(R.string.unable_to_save_stats), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.drive_mode_exit_warning_title)
                .setMessage(R.string.drive_mode_exit_warning_text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveStats();
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
