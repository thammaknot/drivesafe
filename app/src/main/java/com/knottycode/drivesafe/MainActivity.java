package com.knottycode.drivesafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.google.android.gms.internal.zzs.TAG;
import static com.knottycode.drivesafe.R.id.alarmTones;
import static com.knottycode.drivesafe.R.id.checkpointFrequency;
import static com.knottycode.drivesafe.R.id.checkpointFrequencyDisplay;
import static com.knottycode.drivesafe.R.id.recordTone;

public class MainActivity extends Activity {

    TextView checkpointFrequencyTextView;

    protected long checkpointFrequencyMillis;
    protected boolean adaptiveCheckpointFrequency = true;
    protected boolean adaptiveLoudness = true;
    protected Constants.AlertMode alertMode;
    protected List<String> availableAlarmTones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        checkpointFrequencyTextView = (TextView) findViewById(checkpointFrequencyDisplay);
    }

    @Override
    public void onResume() {
        loadPreferences();
        displayPreferences();
        super.onResume();
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
                new ArrayList<String>(prefs.getStringSet(getString(R.string.alarm_tones_key),
                        Constants.allAlarmTones));
        int alertModeCode = prefs.getInt(getString(R.string.alert_style_key),
                Constants.DEFAULT_ALERT_STYLE.getCode());
        alertMode = Constants.AlertMode.fromCode(alertModeCode);
    }

    private void displayPreferences() {
        int checkpointFreqSeconds = (int) (checkpointFrequencyMillis / 1000);
        int minutes = checkpointFreqSeconds / 60;
        int seconds = checkpointFreqSeconds % 60;
        checkpointFrequencyTextView.setText(String.format("%02d:%02d", minutes, seconds));
        if (adaptiveCheckpointFrequency) {
            findViewById(R.id.adaptiveCheckpointFrequencIcon)
                    .setBackground(getResources().getDrawable(R.drawable.adaptive_checkpoint_icon));
            findViewById(R.id.adaptiveCheckpointFrequencIcon).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.adaptiveCheckpointFrequencIcon).setVisibility(View.INVISIBLE);
        }
        switch (alertMode) {
            case SCREEN:
                findViewById(R.id.alertTypeIcon).setVisibility(View.INVISIBLE);
                break;
            case VIBRATE:
                findViewById(R.id.alertTypeIcon).setVisibility(View.INVISIBLE);
                break;
            case SOUND:
                findViewById(R.id.alertTypeIcon).setVisibility(View.VISIBLE);
                break;
            default:
        }
        if (adaptiveLoudness) {
            findViewById(R.id.adaptiveLoudnessIcon)
                    .setBackground(getResources().getDrawable(R.drawable.adaptive_loudness_icon));
            findViewById(R.id.adaptiveLoudnessIcon).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.adaptiveLoudnessIcon).setVisibility(View.INVISIBLE);
        }
    }

    public void enterDriveMode(View view) {
        Intent intent = new Intent(this, DriveModeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void enterSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private String formatTime(int seconds) {
        int minutesPart = seconds / 60;
        int secondsPart = seconds % 60;
        return String.format("%02d:%02d", minutesPart, secondsPart);
    }

    public void showCheckpointFrequencyMenu(View v) {
        final int[] timeOptions = {5, 10, 30, 45, 60, 90, 120, 180, 240, 300};
        final String[] timeOptionsString = new String[timeOptions.length];
        for (int i = 0; i < timeOptions.length; ++i) {
            timeOptionsString[i] = String.valueOf(timeOptions[i]);
        }
        // final TextView checkpointFrequencyDisplay = (TextView) findViewById(R.id.checkpointFrequencyValue);
        new AlertDialog.Builder(this)
                .setTitle(R.string.checkpoint_frequency_menu_title)
                .setItems(timeOptionsString, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs =
                                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(getString(R.string.checkpoint_frequency_key), timeOptions[which]);
                        editor.commit();
                        checkpointFrequencyMillis = timeOptions[which] * 1000;
                        checkpointFrequencyTextView.setText(formatTime(timeOptions[which]));
                    }
                }).show();

    }

    public void toggleAdaptiveCheckpointFrequency(View v) {
        new AlertDialog.Builder(this).setTitle("111").show();
    }

    public void toggleAdaptiveLoudness(View v) {
        new AlertDialog.Builder(this).setTitle("222").show();
    }

    public void showAlertTypeMenu(View v) {
        new AlertDialog.Builder(this).setTitle("333").show();
    }

    public void showToneSelectionMenu(View v) {
        new AlertDialog.Builder(this).setTitle("444").show();
    }

    public void showRecordNewToneMenu(View v) {
        new AlertDialog.Builder(this).setTitle("555").show();
    }

    @Override
    public void onBackPressed() {
        // Exit the app.
        finish();
    }
}
