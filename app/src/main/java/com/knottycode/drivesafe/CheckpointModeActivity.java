package com.knottycode.drivesafe;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by thammaknot on 2/18/17.
 */

public class CheckpointModeActivity extends BaseDriveModeActivity {

    /** Time when we enter checkpoint mode. */
    private long checkpointModeStartTime;

    TextView driveModeTimer;
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
        driveModeTimer = (TextView) findViewById(R.id.driveModeTimer);

        LinearLayout wholeScreenLayout = (LinearLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener((v, me) -> {
            return CheckpointModeActivity.this.onTouch(v, me);
        });
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

    public boolean onTouch(View v, MotionEvent me) {
        long responseTimeMillis = System.currentTimeMillis() - checkpointModeStartTime;
        // checkpointManager.addResponseTime(responseTimeMillis);
        startDriveMode();
        return true;
    }
}
