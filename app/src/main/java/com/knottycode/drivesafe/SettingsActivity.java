package com.knottycode.drivesafe;

import android.content.Intent;
import android.view.View;

import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.knottycode.drivesafe.R.id.adaptiveCheckpointFrequency;
import static com.knottycode.drivesafe.R.id.adaptiveCheckpointFrequencyValue;
import static com.knottycode.drivesafe.R.id.alarmTones;
import static com.knottycode.drivesafe.R.id.checkpointFrequency;
import static com.knottycode.drivesafe.R.id.recordTone;

public class SettingsActivity extends Activity implements View.OnTouchListener {

    private static final String TAG = "SettingsActivity";
    private static String TEMP_RECORDING_FILEPATH = null;
    private static final String MP3_FILE_EXTENSION = ".mp3";
    private SharedPreferences prefs;

    private MediaPlayer mediaPlayer;

    // "filename (e.g., .mp3)" -> "string resource name"
    private Map<String, String> toneDisplayNames;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    private View menuView;
    private ImageButton recordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_settings);

        TEMP_RECORDING_FILEPATH = getFilesDir() + File.separator + "tmp_audio.3gp";

        loadToneDisplayNames();

        mediaPlayer = new MediaPlayer();
        prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        displayCurrentPreferences();

        setupRecordNewToneMenu();
        setOnTouchListeners();

        TextView adaptiveCheckpointFrequencySwitch = (TextView) findViewById(adaptiveCheckpointFrequencyValue);
        TextView adaptiveLoudnessSwitch = (TextView) findViewById(R.id.adaptiveLoudnessValue);
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

    private void setupRecordNewToneMenu() {
        menuView = getLayoutInflater().inflate(R.layout.menu_record_new_tone, null);
        recordButton = (ImageButton) menuView.findViewById(R.id.recordButton);
        final ImageButton playButton = (ImageButton) menuView.findViewById(R.id.playButton);

        final TextView statusTextView = (TextView) menuView.findViewById(R.id.statusIndicatorTextView);

        playButton.setEnabled(false);
        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                if (me.getActionMasked() != MotionEvent.ACTION_UP) {
                    return false;
                }

                if (!isRecording) {
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mediaRecorder.setOutputFile(TEMP_RECORDING_FILEPATH);
                    try {
                        mediaRecorder.prepare();
                        isRecording = true;
                        statusTextView.setText("Recording");
                        playButton.setEnabled(false);
                        mediaRecorder.start();
                    } catch (IOException io) {
                        Log.e(TAG, "Error recording media");
                        io.printStackTrace();
                    }
                } else {
                    stopRecording();
                    playButton.setEnabled(true);
                    statusTextView.setText("NOT Recording");
                }
                return true;
            }
        });
        playButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                if (me.getActionMasked() != MotionEvent.ACTION_UP) {
                    return false;
                }
                if (!isPlaying) {
                    isPlaying = true;
                    statusTextView.setText("Playing");
                    recordButton.setEnabled(false);
                    playSound(TEMP_RECORDING_FILEPATH);
                } else {
                    isPlaying = false;
                    statusTextView.setText("Stopping play");
                    resetMediaPlayer();
                    recordButton.setEnabled(true);
                }
                return true;
            }
        });
    }

    private void setOnTouchListeners() {
        RelativeLayout checkpointFrequency = (RelativeLayout) findViewById(R.id.checkpointFrequency);
        checkpointFrequency.setOnTouchListener(this);

        RelativeLayout alertStyle = (RelativeLayout) findViewById(R.id.alertStyle);
        alertStyle.setOnTouchListener(this);

        RelativeLayout alarmTones = (RelativeLayout) findViewById(R.id.alarmTones);
        alarmTones.setOnTouchListener(this);

        RelativeLayout adaptiveCheckpoint = (RelativeLayout) findViewById(adaptiveCheckpointFrequency);
        adaptiveCheckpoint.setOnTouchListener(this);

        RelativeLayout recordTone = (RelativeLayout) findViewById(R.id.recordTone);
        recordTone.setOnTouchListener(this);

        RelativeLayout adaptiveLoudness = (RelativeLayout) findViewById(R.id.adaptiveLoudness);
        adaptiveLoudness.setOnTouchListener(this);
    }

    private void displayCurrentPreferences() {
        TextView adaptiveCheckpointFrequencyTextView = (TextView) findViewById(adaptiveCheckpointFrequencyValue);
        adaptiveCheckpointFrequencyTextView.setText(
                prefs.getBoolean(getString(R.string.adaptive_checkpoint_frequency_key), true) ? R.string.on : R.string.off);

        TextView adaptiveLoudnessTextView = (TextView) findViewById(R.id.adaptiveLoudnessValue);
        adaptiveLoudnessTextView.setText(
                prefs.getBoolean(getString(R.string.adaptive_loudness_key), true) ? R.string.on : R.string.off);

        TextView checkpointFrequencyTextview = (TextView) findViewById(R.id.checkpointFrequencyValue);
        checkpointFrequencyTextview.setText(
                getCheckpointFrequencyText(prefs.getInt(getString(R.string.checkpoint_frequency_key),
                        Constants.DEFAULT_CHECKPOINT_FREQUENCY_SECONDS),
                        this));

        TextView alertStyleTextView = (TextView) findViewById(R.id.alertStyleValue);
        Constants.AlertMode mode =
                Constants.AlertMode.fromCode(prefs.getInt("alert_style",
                        Constants.DEFAULT_ALERT_STYLE.getCode()));
        alertStyleTextView.setText(mode.getDisplayString(this));

        TextView alarmTonesValueTextView = (TextView) findViewById(R.id.alarmTonesValue);
        Set<String> savedTones = prefs.getStringSet(getString(R.string.alarm_tones_key),
                Constants.allAlarmTones);
        alarmTonesValueTextView.setText(getSelectedTonesMessage(savedTones.size()));

    }

    protected static String getCheckpointFrequencyText(int frequencySeconds, Context context) {
        String minutePart = "";
        if (frequencySeconds > 60) {
            int minutes = frequencySeconds / 60;
            int seconds = frequencySeconds % 60;
            minutePart = String.format(" (%d:%02d " + context.getString(R.string.minutes)
                    + ")", minutes, seconds);
        }
        return String.valueOf(frequencySeconds) + " " + context.getString(R.string.seconds) + minutePart;
    }

    private String getSelectedTonesMessage(int numTones) {
        return String.valueOf(numTones) + " "
                + (numTones == 1 ? getString(R.string.tone_text_singular) :
                                   getString(R.string.tone_text_plural));
    }

    public void onClose(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
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
            case adaptiveCheckpointFrequency:
                toggleAdaptiveCheckpointFrequency();
                break;
            case R.id.alertStyle:
                showAlertStyleMenu();
                break;
            case alarmTones:
                showAlarmTonesMenu();
                break;
            case recordTone:
                showRecordNewToneDialog();
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
        TextView adaptiveCheckpointFrequencyValue =
                (TextView) findViewById(R.id.adaptiveCheckpointFrequencyValue);
        String value = adaptiveCheckpointFrequencyValue.getText().toString();
        int valueId = value.equals(getString(R.string.on)) ? R.string.off : R.string.on;
        adaptiveCheckpointFrequencyValue.setText(valueId);
        SharedPreferences prefs =
                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getString(R.string.adaptive_checkpoint_frequency_key),
                adaptiveCheckpointFrequencyValue.equals(getString(R.string.on)));
        editor.commit();
    }

    private void toggleAdaptiveLoudness() {
        TextView adaptiveLoudnessValue = (TextView) findViewById(R.id.adaptiveLoudnessValue);
        String value = adaptiveLoudnessValue.getText().toString();
        int valueId = value.equals(getString(R.string.on)) ? R.string.off : R.string.on;
        adaptiveLoudnessValue.setText(valueId);
        SharedPreferences prefs =
                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getString(R.string.adaptive_loudness_key),
                adaptiveLoudnessValue.getText().equals(getString(R.string.on)));
        editor.commit();
    }

    private void showCheckpointFrequencyMenu() {
        final int[] timeOptions = {5, 10, 30, 45, 60, 90, 120, 180, 240, 300};
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
                                SettingsActivity.getCheckpointFrequencyText(timeOptions[which],
                                        SettingsActivity.this)) ;
                    }
                }).show();

    }

    private void showAlertStyleMenu() {
        final Constants.AlertMode[] modes = Constants.AlertMode.values();
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
                        editor.putInt(getString(R.string.alert_style_key), modes[which].getCode());
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
                if (allTones[i].endsWith(MP3_FILE_EXTENSION)) {
                    availableAlarmTones.add(allTones[i]);
                }
            }
        } catch (IOException ioe) {
            // Nothing.
        }
        File recordedTone = new File(getRecordedToneFilePath());
        if (recordedTone.exists()) {
            availableAlarmTones.add(Constants.RECORDED_TONE_FILENAME);
        }
        final boolean[] selectedBoolean = new boolean[availableAlarmTones.size()];
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
            } else if (tone.equals(Constants.RECORDED_TONE_FILENAME)) {
                displayString = getString(R.string.recorded_tone_display_name);
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
                                    if (selected.size() == 1) {
                                        selectedBoolean[which] = true;
                                        Toast.makeText(SettingsActivity.this, R.string.zero_tone_warning,
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Else, if the item is already in the array, remove it
                                        selected.remove(tone);
                                    }
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

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        isRecording = false;
    }

    private void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private String getRecordedToneFilePath() {
        return getFilesDir() + File.separator + Constants.RECORDED_TONE_FILENAME;
    }

    private void showRecordNewToneDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.record_new_tone_menu_title)
                .setView(menuView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        File tempFile = new File(TEMP_RECORDING_FILEPATH);
                        File f = new File(getRecordedToneFilePath());
                        try {
                            copyFile(tempFile, f);
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        stopRecording();
                        resetMediaPlayer();
                    }
                })
                .show();
    }

    private void resetMediaPlayer() {
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                isPlaying = false;
                recordButton.setEnabled(true);
            }
        });
    }

    private void playSoundFromDescriptor(FileDescriptor fd) {
        try {
            mediaPlayer.setDataSource(fd);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void playSound(String path) {
        if (mediaPlayer.isPlaying()) {
            resetMediaPlayer();
        }
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
        } catch (IOException ioe) {

        }
    }

    private void playTone(String name) {
        if (mediaPlayer.isPlaying()) {
            resetMediaPlayer();
        }
        if (name.equals(Constants.RECORDED_TONE_FILENAME)) {
            FileDescriptor fd = null;
            try {
                File file = new File(getRecordedToneFilePath());
                FileInputStream fis = new FileInputStream(file);
                fd = fis.getFD();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            }
            playSoundFromDescriptor(fd);
        } else {
            AssetFileDescriptor audioDescriptor = null;
            try {
                audioDescriptor = getAssets().openFd(Constants.ALARM_PATH_PREFIX + File.separator + name);
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
}
