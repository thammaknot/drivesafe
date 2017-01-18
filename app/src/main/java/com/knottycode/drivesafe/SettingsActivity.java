package com.knottycode.drivesafe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.knottycode.drivesafe.R.id.alarmTones;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SettingsActivity";
    private static final int DEFAULT_CHECKPOINT_FREQUENCY_SECONDS = 300;
    private SharedPreferences prefs;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mediaPlayer = new MediaPlayer();
        prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        displayCurrentPreferences();
    }

    private void displayCurrentPreferences() {
        Set<String> savedTones = prefs.getStringSet(getString(R.string.alarm_tones_key),
                new HashSet<String>());
        TextView alarmTonesValueTextView = (TextView) findViewById(R.id.alarmTonesValue);
        alarmTonesValueTextView.setText(savedTones.size() + " tones selected");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.checkpointFrequency:
                showCheckpointFrequencyMenu();
                break;
            case R.id.adaptiveCheckpointFrequency:
                toggleAdaptiveCheckpointFrequency();
                break;
            case R.id.alertStyle:
                showAlertStyleMenu();
                break;
            case alarmTones:
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
        Switch adaptiveCheckpointFrequencySwitch =
                (Switch) findViewById(R.id.adaptiveCheckpointFrequencySwitch);
        adaptiveCheckpointFrequencySwitch.toggle();
        SharedPreferences prefs =
                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getString(R.string.adaptive_checkpoint_frequency_key),
                adaptiveCheckpointFrequencySwitch.isChecked());
        editor.commit();
    }

    private void toggleAdaptiveLoudness() {
        Switch adaptiveLoudnessSwitch = (Switch) findViewById(R.id.adaptiveLoudnessSwitch);
        adaptiveLoudnessSwitch.toggle();
        SharedPreferences prefs =
                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getString(R.string.adaptive_loudness_key),
                adaptiveLoudnessSwitch.isChecked());
        editor.commit();
    }

    private void showCheckpointFrequencyMenu() {
        final View menuView = getLayoutInflater().inflate(R.layout.menu_checkpoint_frequency, null);
        final TextView checkpointFrequencySecondsTextView =
                (TextView) menuView.findViewById(R.id.checkpointFrequencySecondsTextView);
        final TextView checkpointFrequencyValueTextView =
                (TextView) findViewById(R.id.checkpointFrequencyValue);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.checkpoint_frequency_menu_title))
                .setView(menuView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "OK");
                        SharedPreferences prefs =
                                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        int seconds = DEFAULT_CHECKPOINT_FREQUENCY_SECONDS;
                        try {
                            seconds = Integer.parseInt(checkpointFrequencySecondsTextView.getText().toString());
                        } catch (NumberFormatException e) {
                            // Do nothing
                        }
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(getString(R.string.checkpoint_frequency_key), seconds);
                        editor.commit();
                        checkpointFrequencyValueTextView.setText(String.valueOf(seconds + "s"));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "Cancel");
                    }
                }).show();
    }

    private void showAlertStyleMenu() {
        final String[] alertStyles = {"Screen only", "Vibration", "Audio tone"};
        final TextView alertStyleValueTextView =
                (TextView) findViewById(R.id.alertStyleValue);
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_style_menu_title)
                .setItems(alertStyles, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs =
                                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(getString(R.string.checkpoint_frequency_key), alertStyles[which]);
                        editor.commit();
                        alertStyleValueTextView.setText(alertStyles[which]);
                    }
                }).show();
    }

    private void showAlarmTonesMenu() {
        final List<String> availableAlarmTones = new ArrayList<>();
        Set<String> savedTones = prefs.getStringSet(getString(R.string.alarm_tones_key),
                new HashSet<String>());
        try {
            String[] allTones = getAssets().list("");
            for (int i = 0; i < allTones.length; ++i) {
                if (allTones[i].endsWith(".mp3")) {
                    availableAlarmTones.add(allTones[i]);
                }
            }
        } catch (IOException ioe) {
            // Nothing.
        }
        boolean[] selectedBoolean = new boolean[availableAlarmTones.size()];
        int i = 0;
        for (String tone : availableAlarmTones) {
            if (savedTones.contains(tone)) {
                selectedBoolean[i] = true;
            }
            ++i;
        }
        final List<String> selected = new ArrayList<>(savedTones);
        final TextView alarmTonesValueTextView = (TextView) findViewById(R.id.alarmTonesValue);
        final String[] toneArray = new String[availableAlarmTones.size()];
        availableAlarmTones.toArray(toneArray);
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_style_menu_title)
                .setMultiChoiceItems(toneArray, selectedBoolean,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                String tone = toneArray[which];
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    selected.add(tone);
                                    playTone(tone);
                                } else if (selected.contains(tone)) {
                                    // Else, if the item is already in the array, remove it
                                    selected.remove(tone);
                                }
                            }
                        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences prefs =
                                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putStringSet(getString(R.string.alarm_tones_key), new HashSet(selected));
                        editor.commit();
                        alarmTonesValueTextView.setText(selected.size() + " tones selected");
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Nothing
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = new MediaPlayer();
                    }
                })
                .show();
    }

    private void playTone(String name) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = new MediaPlayer();
        }
        AssetFileDescriptor audioDescriptor = null;
        try {
            audioDescriptor = getAssets().openFd(name);
        } catch (IOException ioe) {

        }
        try {
            mediaPlayer.setDataSource(audioDescriptor.getFileDescriptor(),
                    audioDescriptor.getStartOffset(), audioDescriptor.getLength());
            audioDescriptor.close();
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
        } catch (IOException ioe) {

        }
    }
}
