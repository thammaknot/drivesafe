package com.knottycode.drivesafe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DriveModeActivity extends AppCompatActivity {
    private static final String TAG = "DriveModeActivity";

    LinearLayout wholeScreenLayout;

    TextView checkpointCountdownTimer;
    TextView driveModeTimer;

    /** Time when the user last checks in at a checkpoint. */
    private long lastCheckpointTime;
    /** Time when we enter drive (NORMAL) mode. */
    private long driveModeStartTime;
    /** Time when we enter checkpoint mode. */
    private long checkpointModeStartTime;
    private long checkpointFrequencyMillis;

    private boolean adaptiveCheckpointFrequency = true;
    private boolean adaptiveLoudness = true;
    private Set<String> tones;
    private Constants.AlertMode alertMode;

    private CheckpointManager checkpointManager;

    // NORMAL, CHECKPOINT, ALARM
    private String mode = "";
    private static final String NORMAL_MODE = "NORMAL";
    private static final String CHECKPOINT_MODE = "CHECKPOINT";
    private static final String ALARM_MODE = "ALARM";

    private MediaPlayer mediaPlayer;
    private List<String> availableAlarmTones;
    private Random random = new Random();

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
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
            timerHandler.postDelayed(this, Constants.TIMER_INTERVAL_MILLIS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        driveModeStartTime = System.currentTimeMillis();
        loadPreferences();

        checkpointManager =
                new CheckpointManager(checkpointFrequencyMillis, adaptiveCheckpointFrequency);

        mode = NORMAL_MODE;
        // Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_drive_mode);

        wholeScreenLayout = (LinearLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return DriveModeActivity.this.onTouch(v, me);
            }
        });

        checkpointCountdownTimer = (TextView) findViewById(R.id.checkpointCountdownTimer);
        driveModeTimer = (TextView) findViewById(R.id.driveModeTimer);
        // Set up MediaPlayer
        mediaPlayer = new MediaPlayer();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startTimer();
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
        String alertModeString = prefs.getString(getString(R.string.alert_style_key),
                Constants.DEFAULT_ALERT_STYLE.getDisplayString(this));
        alertMode = Constants.AlertMode.fromString(this, alertModeString);
    }

    private void startTimer() {
        lastCheckpointTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void displayNormalMode() {
        mode = NORMAL_MODE;
        TextView message = (TextView) findViewById(R.id.timeUntilNextCheckpointTextView);
        message.setText(R.string.time_until_next_checkpoint);
        lastCheckpointTime = System.currentTimeMillis();
        checkpointFrequencyMillis = checkpointManager.getNextFrequencyMillis();
        wholeScreenLayout.setBackgroundColor(Color.BLACK);
        checkpointCountdownTimer.setTextColor(Color.YELLOW);
        driveModeTimer.setTextColor(Color.YELLOW);
    }

    private void displayCheckpointMode() {
        mode = CHECKPOINT_MODE;
        TextView message = (TextView) findViewById(R.id.timeUntilNextCheckpointTextView);
        message.setText(R.string.time_until_alarm);
        checkpointModeStartTime = System.currentTimeMillis();
        wholeScreenLayout.setBackgroundColor(Color.YELLOW);
        checkpointCountdownTimer.setTextColor(Color.BLACK);
        driveModeTimer.setTextColor(Color.BLACK);
        Toast.makeText(this, "Mode: " + alertMode.getDisplayString(this), Toast.LENGTH_SHORT);
        switch (alertMode) {
            case SCREEN:
                // nothing
                break;
            case VIBRATE:
                vibrate();
                break;
            case SOUND:
                playAlert();
                break;
            default:
        }
    }

    private void vibrate() {
        Vibrator v = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(Constants.VIBRATION_PATTERN, -1);
    }

    private void playAlert() {
        try {
            AssetFileDescriptor afd = getAssets().openFd(Constants.DEFAULT_ALERT_SOUND);
            mediaPlayer.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(), afd.getLength());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            afd.close();
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(Constants.ALERT_VOLUME, Constants.ALERT_VOLUME);
            new CountDownTimer(1000, 10000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // Nothing to do
                }

                @Override
                public void onFinish() {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                    mediaPlayer = new MediaPlayer();
                    this.cancel();
                }
            }.start();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayAlarmMode() {
        mode = ALARM_MODE;
        wholeScreenLayout.setBackgroundColor(Color.RED);
        checkpointCountdownTimer.setTextColor(Color.YELLOW);
        driveModeTimer.setTextColor(Color.YELLOW);

        AssetFileDescriptor audioDescriptor = getAudioDescriptor();
        try {
            mediaPlayer.setDataSource(audioDescriptor.getFileDescriptor(),
                    audioDescriptor.getStartOffset(), audioDescriptor.getLength());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            audioDescriptor.close();
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean onTouch(View v, MotionEvent me) {
        if (mode.equals(CHECKPOINT_MODE) || mode.equals(ALARM_MODE)) {
            if (mode.equals(ALARM_MODE)) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = new MediaPlayer();
            }
            long responseTimeMillis = System.currentTimeMillis() - checkpointModeStartTime;
            checkpointManager.addResponseTime(responseTimeMillis);
            displayNormalMode();
        } else if (mode.equals(NORMAL_MODE)) {
            // Do nothing.
        }
        return true;
    }

    private AssetFileDescriptor getAudioDescriptor() {
        AssetFileDescriptor afd = null;
        String tone = availableAlarmTones.get(random.nextInt(availableAlarmTones.size()));
        try {
            afd = getAssets().openFd(tone);
        } catch (IOException ioe) {
            return null;
        }
        return afd;
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.drive_mode_exit_warning_title)
                .setMessage(R.string.drive_mode_exit_warning_text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        DriveModeActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
