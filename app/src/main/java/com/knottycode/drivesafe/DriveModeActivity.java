package com.knottycode.drivesafe;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DriveModeActivity extends BaseDriveModeActivity {
    TextView checkpointCountdownTimer;
    TextView driveModeTimer;

    /** Time when the user last checks in at a checkpoint. */
    private long lastCheckpointTime;
    /** Time when we enter drive (NORMAL) mode. */
    private long driveModeStartTime;

    private CheckpointManager checkpointManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = NORMAL_MODE;
        TAG = "DriveSafe: DriveModeActivity";
        init();

        driveModeStartTime = System.currentTimeMillis();
        checkpointManager =
                new CheckpointManager(checkpointFrequencyMillis, adaptiveCheckpointFrequency);

        setContentView(R.layout.activity_drive_mode);

        LinearLayout wholeScreenLayout = (LinearLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener((v, me) -> {
            return DriveModeActivity.this.onTouch(v, me);
        });

        checkpointCountdownTimer = (TextView) findViewById(R.id.checkpointCountdownTimer);
        driveModeTimer = (TextView) findViewById(R.id.driveModeTimer);
    }

    @Override
    public void onResume() {
        lastCheckpointTime = System.currentTimeMillis();
        super.onResume();
    }

    @Override
    protected void updateDisplay(long now) {
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
    }

    @Override
    protected boolean proceedToNextStep(long now) {
        long millis = now - lastCheckpointTime;

        if (millis >= checkpointFrequencyMillis) {
            Log.d(TAG, "#### Starting checkpoint mode!!!");
            startCheckpointMode();
            return true;
        }
        return false;
    }

    /*
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
    */
    /*
    private void displayCheckpointMode() {
        mode = CHECKPOINT_MODE;
        validateSystemLoudness();
        TextView message = (TextView) findViewById(R.id.timeUntilNextCheckpointTextView);
        message.setText(R.string.time_until_alarm);
        checkpointModeStartTime = System.currentTimeMillis();
        wholeScreenLayout.setBackgroundColor(Color.YELLOW);
        checkpointCountdownTimer.setTextColor(Color.BLACK);
        driveModeTimer.setTextColor(Color.BLACK);
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
    */

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
            mediaPlayer.setAudioStreamType(ALARM_STREAM);
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
        Intent intent = new Intent(this, AlarmModeActivity.class);
        startActivity(intent);
        /*
        mode = ALARM_MODE;
        wholeScreenLayout.setBackgroundColor(Color.RED);
        checkpointCountdownTimer.setTextColor(Color.YELLOW);
        driveModeTimer.setTextColor(Color.YELLOW);

        FileDescriptor audioDescriptor = getAudioDescriptor();
        try {
            mediaPlayer.setDataSource(audioDescriptor);
            mediaPlayer.setAudioStreamType(ALARM_STREAM);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
            if (adaptiveLoudness) {
                startAdaptiveLoudnessTimer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    private boolean onTouch(View v, MotionEvent me) {
        return true;
    }

    /*
    private FileDescriptor getAudioDescriptor() {
        FileDescriptor afd = null;
        String tone = availableAlarmTones.get(random.nextInt(availableAlarmTones.size()));
        if (tone.equals(Constants.RECORDED_TONE_FILENAME)) {
            try {
                File f = new File(getRecordedToneFilePath());
                FileInputStream fis = new FileInputStream(f);
                return fis.getFD();
            } catch (IOException ioe) {

            }
        } else {
            try {
                afd = getAssets().openFd(Constants.ALARM_PATH_PREFIX + "/" + tone).getFileDescriptor();
            } catch (IOException ioe) {
                return null;
            }
        }
        return afd;
    }
    */
}
