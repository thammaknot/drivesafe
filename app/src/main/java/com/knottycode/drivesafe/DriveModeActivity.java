package com.knottycode.drivesafe;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DriveModeActivity extends BaseDriveModeActivity {
    TextView checkpointCountdownTimer;

    /** Time when the user last checks in at a checkpoint. */
    private long lastCheckpointTime;

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
        Log.d(TAG, "******** volume adjusted = " + checkpointManager.getVolumeAdjusted());
        if (!checkpointManager.getVolumeAdjusted()) {
            validateSystemLoudness();
            checkpointManager.setVolumeAdjusted(true);
        }
    }

    @Override
    protected void updateDisplay(long now) {
        long millis = now - lastCheckpointTime;
        millis = checkpointFrequencyMillis - millis;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        checkpointCountdownTimer.setText(String.format("%d:%02d", minutes, seconds));
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
