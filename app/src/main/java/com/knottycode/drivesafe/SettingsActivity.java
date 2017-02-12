package com.knottycode.drivesafe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.os.Build.ID;
import static com.knottycode.drivesafe.R.id.alarmTones;
import static com.knottycode.drivesafe.R.id.checkpointFrequency;

public class SettingsActivity extends AppCompatActivity implements View.OnTouchListener {

    private static final String TAG = "SettingsActivity";
    private SharedPreferences prefs;

    private MediaPlayer mediaPlayer;

    // "filename (e.g., .mp3)" -> "string resource name"
    private Map<String, String> toneDisplayNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        loadToneDisplayNames();

        mediaPlayer = new MediaPlayer();
        prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        displayCurrentPreferences();

        setOnTouchListeners();

        Switch adaptiveCheckpointFrequencySwitch = (Switch) findViewById(R.id.adaptiveCheckpointFrequencySwitch);
        adaptiveCheckpointFrequencySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(getString(R.string.adaptive_checkpoint_frequency_key), checked);
                editor.commit();
            }
        });

        Switch adaptiveLoudnessSwitch = (Switch) findViewById(R.id.adaptiveLoudnessSwitch);
        adaptiveLoudnessSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(getString(R.string.adaptive_loudness_key), checked);
                editor.commit();
            }
        });
    }

    private void loadToneDisplayNames() {
        toneDisplayNames = new HashMap<String, String>();
        AssetManager am = getAssets();
        InputStream is;
        try {
            is = am.open(Constants.ALARM_PATH_PREFIX + "/tone_index.txt");
        } catch (IOException io) {
            // Leave the map empty.
            return;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = r.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != 2) {
                    Log.w(TAG, "Malformed line in tone index file: " + line);
                    continue;
                }
                toneDisplayNames.put(tokens[0], tokens[1]);
            }
        } catch (IOException io) {
            Log.e(TAG, "Exception while reading tone index file.");
            io.printStackTrace();
        }
    }

    private void setOnTouchListeners() {
        RelativeLayout checkpointFrequency = (RelativeLayout) findViewById(R.id.checkpointFrequency);
        checkpointFrequency.setOnTouchListener(this);

        RelativeLayout alertStyle = (RelativeLayout) findViewById(R.id.alertStyle);
        alertStyle.setOnTouchListener(this);

        RelativeLayout alarmTones = (RelativeLayout) findViewById(R.id.alarmTones);
        alarmTones.setOnTouchListener(this);

        RelativeLayout adaptiveCheckpoint = (RelativeLayout) findViewById(R.id.adaptiveCheckpointFrequency);
        adaptiveCheckpoint.setOnTouchListener(this);

        RelativeLayout adaptiveLoudness = (RelativeLayout) findViewById(R.id.adaptiveLoudness);
        adaptiveLoudness.setOnTouchListener(this);
    }

    private void displayCurrentPreferences() {
        Switch adaptiveCheckpointFrequencySwitch = (Switch) findViewById(R.id.adaptiveCheckpointFrequencySwitch);
        adaptiveCheckpointFrequencySwitch.setChecked(
                prefs.getBoolean(getString(R.string.adaptive_checkpoint_frequency_key), true));

        Switch adaptiveLoudnessSwitch = (Switch) findViewById(R.id.adaptiveLoudnessSwitch);
        adaptiveLoudnessSwitch.setChecked(
                prefs.getBoolean(getString(R.string.adaptive_loudness_key), true));

        TextView checkpointFrequencyTextview = (TextView) findViewById(R.id.checkpointFrequencyValue);
        checkpointFrequencyTextview.setText(
                getCheckpointFrequencyText(prefs.getInt(getString(R.string.checkpoint_frequency_key),
                        Constants.DEFAULT_CHECKPOINT_FREQUENCY_SECONDS)));

        TextView alertStyleTextView = (TextView) findViewById(R.id.alertStyleValue);
        alertStyleTextView.setText(prefs.getString(getString(R.string.alert_style_key),
                Constants.DEFAULT_ALERT_STYLE.getDisplayString(this)));

        TextView alarmTonesValueTextView = (TextView) findViewById(R.id.alarmTonesValue);
        Set<String> savedTones = prefs.getStringSet(getString(R.string.alarm_tones_key),
                new HashSet<String>());
        alarmTonesValueTextView.setText(getSelectedTonesMessage(savedTones.size()));

    }

    private String getCheckpointFrequencyText(int frequencySeconds) {
        String minutePart = "";
        if (frequencySeconds > 60) {
            int minutes = frequencySeconds / 60;
            int seconds = frequencySeconds % 60;
            minutePart = String.format(" (%d:%02d " + getString(R.string.minutes)
                    + ")", minutes, seconds);
        }
        return String.valueOf(frequencySeconds) + " " + getString(R.string.seconds) + minutePart;
    }

    private String getSelectedTonesMessage(int numTones) {
        return String.valueOf(numTones) + " "
                + (numTones == 1 ? getString(R.string.tone_text_singular) :
                                   getString(R.string.tone_text_plural));
    }

    @Override
    public boolean onTouch(View v, MotionEvent me) {
        if (me.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }
        switch (v.getId()) {
            case checkpointFrequency:
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
        return false;
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
        final int[] timeOptions = {30, 45, 60, 90, 120, 180, 240, 300};
        final String[] timeOptionsString = new String[timeOptions.length];
        for (int i = 0; i < timeOptions.length; ++i) {
            timeOptionsString[i] = String.valueOf(timeOptions[i]);
        }
        final TextView checkpointFrequencyTextView =
                (TextView) findViewById(R.id.checkpointFrequencyValue);
        new AlertDialog.Builder(this)
                .setTitle(R.string.checkpoint_frequency_menu_title)
                .setItems(timeOptionsString, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs =
                                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(getString(R.string.checkpoint_frequency_key), timeOptions[which]);
                        editor.commit();
                        checkpointFrequencyTextView.setText(
                                SettingsActivity.this.getCheckpointFrequencyText(timeOptions[which])) ;
                    }
                }).show();

    }

    private void showAlertStyleMenu() {
        Constants.AlertMode[] modes = Constants.AlertMode.values();
        final String[] alertStyles = new String[modes.length];
        for (int i = 0; i < modes.length; ++i) {
            alertStyles[i] = modes[i].getDisplayString(this);
        }
        final TextView alertStyleValueTextView =
                (TextView) findViewById(R.id.alertStyleValue);
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_style_menu_title)
                .setItems(alertStyles, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs =
                                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(getString(R.string.alert_style_key), alertStyles[which]);
                        editor.commit();
                        alertStyleValueTextView.setText(alertStyles[which]);
                    }
                }).show();
    }

    private void showAlarmTonesMenu() {
        // List of (mp3) filenames found in asset directory.
        final List<String> availableAlarmTones = new ArrayList<>();
        Set<String> savedTones = prefs.getStringSet(getString(R.string.alarm_tones_key),
                new HashSet<String>());
        try {
            String[] allTones = getAssets().list(Constants.ALARM_PATH_PREFIX);
            for (int i = 0; i < allTones.length; ++i) {
                if (allTones[i].endsWith(".mp3")) {
                    // String displayName = getDisplayToneName(allTones[i]);
                    availableAlarmTones.add(allTones[i]);
                }
            }
        } catch (IOException ioe) {
            // Nothing.
        }
        boolean[] selectedBoolean = new boolean[availableAlarmTones.size()];
        int i = 0;
        final String[] toneLocalizedDisplayStrings = new String[availableAlarmTones.size()];
        for (String tone : availableAlarmTones) {
            if (savedTones.contains(tone)) {
                selectedBoolean[i] = true;
            }
            String stringId = toneDisplayNames.get(tone);
            String displayString = tone;
            if (stringId != null) {
                int resId = getResources().getIdentifier(stringId, "string", this.getClass().getPackage().getName());
                if (resId > 0) {
                    displayString = getString(resId);
                }
            }
            toneLocalizedDisplayStrings[i] = displayString;
            ++i;
        }
        final List<String> selected = new ArrayList<>(savedTones);
        final TextView alarmTonesValueTextView = (TextView) findViewById(R.id.alarmTonesValue);
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_style_menu_title)
                .setMultiChoiceItems(toneLocalizedDisplayStrings, selectedBoolean,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                String tone = availableAlarmTones.get(which);
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    selected.add(tone);
                                    playTone(tone);
                                } else if (selected.contains(tone)) {
                                    // Else, if the item is already in the array, remove it
                                    selected.remove(tone);
                                    resetMediaPlayer();
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
                        alarmTonesValueTextView.setText(getSelectedTonesMessage(selected.size()));
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
                        resetMediaPlayer();
                    }
                })
                .show();
    }

    private void resetMediaPlayer() {
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = new MediaPlayer();
    }

    private void playTone(String name) {
        if (mediaPlayer.isPlaying()) {
            resetMediaPlayer();
        }
        AssetFileDescriptor audioDescriptor = null;
        try {
            audioDescriptor = getAssets().openFd(Constants.ALARM_PATH_PREFIX + "/" + name);
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to open audio descriptor for " + name);
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
