package com.knottycode.drivesafe;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by thammaknot on 2/18/17.
 */

public class CheckpointModeActivity extends BaseDriveModeActivity {

    /** Time when we enter checkpoint mode. */
    private long checkpointModeStartTime;

    TextView checkpointCountdownTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = CHECKPOINT_MODE;
        TAG = "DriveSafe: CheckpointModeActivity";
        init();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_checkpoint_mode);
        checkpointCountdownTimer = (TextView) findViewById(R.id.checkpointCountdownTimer);

        RelativeLayout wholeScreenLayout = (RelativeLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return CheckpointModeActivity.this.onTouch(v, me);
            }
        });
        executeAlert();
        startVoiceRecognitionActivity();
    }

    @Override
    public void onResume() {
        checkpointModeStartTime = System.currentTimeMillis();
        super.onResume();
    }

    @Override
    protected void updateDisplay(long now) {
        long checkpointElapsed = now - checkpointModeStartTime;
        checkpointElapsed = Constants.CHECKPOINT_GRACE_PERIOD_MILLIS - checkpointElapsed;
        int seconds = (int) (checkpointElapsed / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        checkpointCountdownTimer.setText(String.format("%d:%02d", minutes, seconds));
    }

    @Override
    protected boolean proceedToNextStep(long now) {
        long checkpointElapsed = now - checkpointModeStartTime;
        if (checkpointElapsed >= Constants.CHECKPOINT_GRACE_PERIOD_MILLIS) {
            startAlarmMode();
            return true;
        }
        return false;
    }

    @Override
    public void onASRResultsReady(List<String> results) {
        if (results.contains(Constants.SAFE_PHRASE)) {
            long responseTimeMillis = System.currentTimeMillis() - checkpointModeStartTime;
            checkpointManager.addResponseTime(responseTimeMillis);
            startDriveMode();
        } else {
            Log.d(TAG, "##### SAFETY PHRASE not found!!!!");
        }
    }

    public boolean onTouch(View v, MotionEvent me) {
        if (me.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }
        long responseTimeMillis = System.currentTimeMillis() - checkpointModeStartTime;
        checkpointManager.addResponseTime(responseTimeMillis);
        startDriveMode();
        return true;
    }

    private void vibrate() {
        Vibrator v = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
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

    private void executeAlert() {
        switch (alertMode) {
            case SCREEN:
                break;
            case SOUND:
                playAlert();
                break;
            case VIBRATE:
                vibrate();
                break;
            default:
                break;
        }
    }
}
