package com.knottycode.drivesafe;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DriveModeActivity extends AppCompatActivity {
    private static final String TAG = "DriveModeActivity";

    LinearLayout wholeScreenLayout;

    TextView checkpointCountdownTimer;
    TextView driveModeTimer;

    private static final int TIMER_INTERVAL_MILLIS = 100;
    private long lastCheckpointTime;
    private long driveModeStartTime;
    private long checkpointFrequencyMillis = 15 * 1000;

    // NORMAL, CHECKPOINT, ALARM
    private String mode = "";
    private static final String NORMAL_MODE = "NORMAL";
    private static final String CHECKPOINT_MODE = "CHECKPOINT";
    private static final String ALARM_MODE = "ALARM";

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

            } else if (mode.equals(ALARM_MODE)) {

            }
            timerHandler.postDelayed(this, TIMER_INTERVAL_MILLIS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        driveModeStartTime = System.currentTimeMillis();
        mode = NORMAL_MODE;
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
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
        startTimer();
    }

    private void startTimer() {
        lastCheckpointTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void displayNormalMode() {
        mode = NORMAL_MODE;
        lastCheckpointTime = System.currentTimeMillis();
    }

    private void displayCheckpointMode() {
        mode = CHECKPOINT_MODE;
        long checkpointModeStartTime = System.currentTimeMillis();
        Log.d(TAG, "Checkpoint MODE");
    }

    private boolean onTouch(View v, MotionEvent me) {
        Toast.makeText(DriveModeActivity.this, "Touch detected in " + mode + " mode", Toast.LENGTH_SHORT).show();
        if (mode.equals(CHECKPOINT_MODE)) {
            displayNormalMode();
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit drive mode")
                .setMessage("Are you sure you want to exit drive mode?")
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
