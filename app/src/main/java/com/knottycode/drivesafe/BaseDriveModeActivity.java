package com.knottycode.drivesafe;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
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
    protected CheckpointManager checkpointManager;

    protected int initialVolume;

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
        startTimer();
    }

    @Override
    public void onPause() {
        audioManager.setStreamVolume(ALARM_STREAM, initialVolume, 0);
        super.onPause();
    }

    @Override
    public void onResume() {
        initialVolume = audioManager.getStreamVolume(ALARM_STREAM);
        validateSystemLoudness();
        super.onResume();
    }

    /**
     * Is the screen of the device on.
     * @param context the context
     * @return true when (at least one) screen is on
     */
    public boolean isScreenOn() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm.isInteractive();
        }
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
        stopTimer();
        Intent intent = new Intent(this, AlarmModeActivity.class);
        startActivity(intent);
    }

    protected void startCheckpointMode() {
        stopTimer();
        Intent intent = new Intent(this, CheckpointModeActivity.class);
        startActivity(intent);
    }

    protected void startDriveMode() {
        stopTimer();
        Intent intent = new Intent(this, DriveModeActivity.class);
        startActivity(intent);
    }

    private void goBackToMainActivity() {
        stopTimer();
        checkpointManager.invalidateSingletonInstance();
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
