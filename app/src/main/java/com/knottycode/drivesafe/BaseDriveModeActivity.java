package com.knottycode.drivesafe;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by thammaknot on 2/18/17.
 */

abstract public class BaseDriveModeActivity extends AppCompatActivity {
    protected String TAG = "BaseDriveModeActivity";
    protected static final int ADAPTIVE_LOUDNESS_INTERVAL_MILLIS = 3000;
    protected static final int ADAPTIVE_LOUDNESS_COUNTDOWN_DURATION = 20000;
    protected static final int ALARM_STREAM = AudioManager.STREAM_ALARM;

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

    protected int initialVolume;

    protected Handler timerHandler = new Handler();
    protected Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "********* Inside runnable");
            long now = System.currentTimeMillis();
            updateDisplay(now);
            if (!proceedToNextStep(now)) {
                timerHandler.postDelayed(this, Constants.TIMER_INTERVAL_MILLIS);
            }
            /*
            if (mode.equals(NORMAL_MODE)) {
                long millis = now - lastCheckpointTime;
                millis = checkpointFrequencyMillis - millis;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                checkpointCountdownTimer.setText(String.format("%d:%02d", minutes, seconds));

                long timeSinceStart = now - driveModeStartTime;
                seconds = (int) (timeSinceStart / 1000);
                minutes = seconds / 60;
                seconds = seconds % 60;
                int hours = minutes / 60;
                minutes = minutes % 60;

                if (hours > 0) {
                    driveModeTimer.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
                } else {
                    driveModeTimer.setText(String.format("%d:%02d", minutes, seconds));
                }

                if (millis <= 0) {
                    displayCheckpointMode();
                }
            } else if (mode.equals(CHECKPOINT_MODE)) {
                long checkpointElapsed = now - checkpointModeStartTime;
                int seconds = (int) (checkpointElapsed / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                checkpointCountdownTimer.setText(String.format("%d:%02d", minutes, seconds));

                if (checkpointElapsed >= Constants.CHECKPOINT_GRACE_PERIOD_MILLIS) {
                    displayAlarmMode();
                }
            } else if (mode.equals(ALARM_MODE)) {

            }
            */
        }
    };

    protected void init() {
        // Initialize screen state receiver.
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenStateReceiver();
        registerReceiver(mReceiver, filter);

        // Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        loadPreferences();
        mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "@@@ Inside onPause... screen on is " + ScreenStateReceiver.screenOn);
        if (false && ScreenStateReceiver.screenOn) {
            // Screen about to turn off.
        } else {
            Log.d(TAG, "@@@ Inside onPause...STOPPING TIMER");
            stopTimer();
            audioManager.setStreamVolume(ALARM_STREAM, initialVolume, 0);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "@@@ Inside onResume");
        if (false && !ScreenStateReceiver.screenOn) {
            // Screen about to turn on.
        } else {
            initialVolume = audioManager.getStreamVolume(ALARM_STREAM);
            Log.d(TAG, "@@@ Inside onResume: STARTING TIMER");
            validateSystemLoudness();
            startTimer();
        }
        super.onResume();
    }

    private void startTimer() {
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopTimer() {
        Log.d(TAG, "### stopping timer!!!!");
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
                new ArrayList<String>(prefs.getStringSet(getString(R.string.alarm_tones_key), new HashSet()));
        int alertModeCode = prefs.getInt(getString(R.string.alert_style_key),
                Constants.DEFAULT_ALERT_STYLE.getCode());
        alertMode = Constants.AlertMode.fromCode(alertModeCode);
    }

    abstract protected void updateDisplay(long now);

    abstract protected boolean proceedToNextStep(long now);

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
        Intent intent = new Intent(this, AlarmModeActivity.class);
        startActivity(intent);
    }

    protected void startCheckpointMode() {
        Intent intent = new Intent(this, CheckpointModeActivity.class);
        startActivity(intent);
    }

    protected void startDriveMode() {
        Intent intent = new Intent(this, DriveModeActivity.class);
        startActivity(intent);
    }

    private void goBackToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.drive_mode_exit_warning_title)
                .setMessage(R.string.drive_mode_exit_warning_text)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        goBackToMainActivity();
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> { })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
