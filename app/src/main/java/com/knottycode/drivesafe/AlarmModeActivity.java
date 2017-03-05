package com.knottycode.drivesafe;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Created by thammaknot on 2/18/17.
 */

public class AlarmModeActivity extends BaseDriveModeActivity {

    private MediaPlayer mediaPlayer;
    CountDownTimer adaptiveLoudnessTimer;

    private long alarmModeStartTime;

    private Random random = new Random();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = ALARM_MODE;
        TAG = "DriveSafe: AlarmModeActivity";
        init();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                             WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_alarm_mode);
        RelativeLayout wholeScreenLayout = (RelativeLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return AlarmModeActivity.this.onTouch(v, me);
            }
        });
        startAlarm();
        startVoiceRecognitionActivity();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAlarm();
    }

    @Override
    public void onResume() {
        alarmModeStartTime = System.currentTimeMillis();
        super.onResume();
    }

    private boolean onTouch(View v, MotionEvent me) {
        if (me.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }
        long responseTimeMillis = System.currentTimeMillis() - alarmModeStartTime
                + Constants.CHECKPOINT_GRACE_PERIOD_MILLIS;
        checkpointManager.addResponseTime(responseTimeMillis);
        stopAlarm();
        startDriveMode();
        return true;
    }

    private String getRecordedToneFilePath() {
        return getFilesDir() + File.separator + Constants.RECORDED_TONE_FILENAME;
    }

    private void startAlarm() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        String tone = availableAlarmTones.get(random.nextInt(availableAlarmTones.size()));

        if (tone.equals(Constants.RECORDED_TONE_FILENAME)) {
            try {
                File f = new File(getRecordedToneFilePath());
                FileInputStream fis = new FileInputStream(f);
                mediaPlayer.setDataSource(fis.getFD());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            try {
                AssetFileDescriptor afd = getAssets().openFd(Constants.ALARM_PATH_PREFIX + "/" + tone);
                mediaPlayer.setDataSource(afd.getFileDescriptor(),
                        afd.getStartOffset(), afd.getLength());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        try {
            mediaPlayer.setAudioStreamType(ALARM_STREAM);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = new MediaPlayer();
            }
        }
        if (adaptiveLoudnessTimer != null) {
            adaptiveLoudnessTimer.cancel();
            audioManager.setStreamVolume(ALARM_STREAM, initialVolume, 0);
        }
    }

    @Override
    protected void updateDisplay(long now) {}

    @Override
    protected boolean proceedToNextStep(long now) { return false; }

    @Override
    public void onASRResultsReady(List<String> results) {
        if (results.contains(Constants.SAFE_PHRASE)) {
            long responseTimeMillis = System.currentTimeMillis() - alarmModeStartTime
                    + Constants.CHECKPOINT_GRACE_PERIOD_MILLIS;
            checkpointManager.addResponseTime(responseTimeMillis);
            stopAlarm();
            startDriveMode();
        } else {
            Log.d(TAG, "##### SAFETY PHRASE not found!!!!");
        }
    }

}
