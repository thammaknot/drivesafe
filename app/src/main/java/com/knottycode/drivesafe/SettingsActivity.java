package com.knottycode.drivesafe;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.checkpointFrequency:
                showCheckpointFreuquencyMenu();
                break;
            case R.id.adaptiveCheckpointFrequency:
                toggleAdaptiveCheckpointFrequency();
                break;
            case R.id.alertStyle:
                showAlertStyleMenu();
                break;
            case R.id.alarmTones:
                showAlarmTonesMenu();
                break;
            case R.id.adaptiveLoudness:
                toggleAdaptiveLoudness();
                break;
            default:
                break;
        }
    }

    private void toggleAdaptiveCheckpointFrequency() {
        Switch adaptiveCheckpointFrequencySwitch = (Switch) findViewById(R.id.adaptiveCheckpointFrequencySwitch);
        adaptiveCheckpointFrequencySwitch.toggle();
    }

    private void toggleAdaptiveLoudness() {
        Switch adaptiveLoudnessSwitch = (Switch) findViewById(R.id.adaptiveLoudnessSwitch);
        adaptiveLoudnessSwitch.toggle();
    }

    private void showCheckpointFreuquencyMenu() {
        new AlertDialog.Builder(this)
                .setTitle("Set Checkpoint Frequency")
                .setView(getLayoutInflater().inflate(R.layout.menu_checkpoint_frequency, null))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "OK");
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "Cancel");
                    }
                }).show();
    }

    private void showAlertStyleMenu() {

    }

    private void showAlarmTonesMenu() {

    }
}
