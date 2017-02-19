package com.knottycode.drivesafe;

import android.os.Bundle;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = NORMAL_MODE;
        TAG = "DriveSafe: DriveModeActivity";
        lastCheckpointTime = System.currentTimeMillis();
        init();

        setContentView(R.layout.activity_drive_mode);

        LinearLayout wholeScreenLayout = (LinearLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener((v, me) -> {
            return DriveModeActivity.this.onTouch(v, me);
        });

        checkpointCountdownTimer = (TextView) findViewById(R.id.checkpointCountdownTimer);
        driveModeTimer = (TextView) findViewById(R.id.driveModeTimer);
    }

    @Override
    protected void updateDisplay(long now) {
        long millis = now - lastCheckpointTime;
        millis = checkpointFrequencyMillis - millis;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        checkpointCountdownTimer.setText(String.format("%d:%02d", minutes, seconds));

        long timeSinceStart = now - checkpointManager.getDriveModeStartTime();
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
            startCheckpointMode();
            return true;
        }
        return false;
    }

    private boolean onTouch(View v, MotionEvent me) {
        return true;
    }
}
