package com.knottycode.drivesafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.knottycode.drivesafe.R.id.alarmTones;
import static com.knottycode.drivesafe.R.id.checkpointFrequency;
import static com.knottycode.drivesafe.R.id.history;
import static com.knottycode.drivesafe.R.id.logout;
import static com.knottycode.drivesafe.R.id.questionTypes;
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
    private View historyView;
    private ImageButton recordButton;

    private static final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

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

        setOnTouchListeners();
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
                        playButton.setEnabled(false);
                        mediaRecorder.start();
                    } catch (IOException io) {
                        Log.e(TAG, "Error recording media");
                        io.printStackTrace();
                    }
                } else {
                    stopRecording();
                    playButton.setEnabled(true);
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
                    recordButton.setEnabled(false);
                    playSound(TEMP_RECORDING_FILEPATH);
                } else {
                    isPlaying = false;
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

        RelativeLayout questionTypes = (RelativeLayout) findViewById(R.id.questionTypes);
        questionTypes.setOnTouchListener(this);

        RelativeLayout alarmTones = (RelativeLayout) findViewById(R.id.alarmTones);
        alarmTones.setOnTouchListener(this);

        RelativeLayout recordTone = (RelativeLayout) findViewById(R.id.recordTone);
        recordTone.setOnTouchListener(this);

        RelativeLayout history = (RelativeLayout) findViewById(R.id.history);
        history.setOnTouchListener(this);

        RelativeLayout logout = (RelativeLayout) findViewById(R.id.logout);
        logout.setOnTouchListener(this);
    }

    private void displayCurrentPreferences() {
        TextView checkpointFrequencyTextview = (TextView) findViewById(R.id.checkpointFrequencyValue);
        checkpointFrequencyTextview.setText(
                getCheckpointFrequencyText(prefs.getInt(getString(R.string.checkpoint_frequency_key),
                        Constants.DEFAULT_CHECKPOINT_FREQUENCY_SECONDS),
                        this));

        TextView alarmTonesValueTextView = (TextView) findViewById(R.id.alarmTonesValue);
        Set<String> savedTones = prefs.getStringSet(getString(R.string.alarm_tones_key),
                Constants.allAlarmTones);
        alarmTonesValueTextView.setText(getSelectedTonesMessage(savedTones.size()));
        TextView questionTypeTextView = (TextView) findViewById(R.id.selectQuestionTypeValue);
        Set<String> savedTypes = prefs.getStringSet(getString(R.string.question_types_key),
                Constants.ALL_QUESTION_TYPES);
        questionTypeTextView.setText(getSelectedQuestionTypesMessage(savedTypes.size()));
    }

    protected static String getNaturalLanguageText(int frequencySeconds, Context context) {
        int minutes = frequencySeconds / 60;
        int seconds = frequencySeconds % 60;
        String output = "";
        if (minutes > 0) {
            output = String.valueOf(minutes) + " " + context.getString(R.string.minutes) + " ";
        }
        if (seconds > 0) {
            String unit = minutes > 0 ?
                    context.getString(R.string.seconds_abbrev) : context.getString(R.string.seconds);
            output += seconds + " " + unit;
        }
        return output;
    }

    protected static String getCheckpointFrequencyText(int frequencySeconds, Context context) {
        String minutePart = "";
        if (frequencySeconds > 60) {
            int minutes = frequencySeconds / 60;
            int seconds = frequencySeconds % 60;
            minutePart = String.format(" (%d:%02d " + context.getString(minutes)
                    + ")", minutes, seconds);
        }
        return String.valueOf(frequencySeconds) + " " + context.getString(R.string.seconds) + minutePart;
    }

    private String getSelectedTonesMessage(int numTones) {
        return String.valueOf(numTones) + " "
                + (numTones == 1 ? getString(R.string.tone_text_singular) :
                                   getString(R.string.tone_text_plural));
    }

    private String getSelectedQuestionTypesMessage(int numTypes) {
        return String.valueOf(numTypes) + " "
                + (numTypes == 1 ? getString(R.string.type_text_singular) :
                getString(R.string.type_text_plural));
    }

    public void onClose(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
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
            case questionTypes:
                showQuestionTypesMenu();
                break;
            case alarmTones:
                showAlarmTonesMenu();
                break;
            case recordTone:
                showRecordNewToneDialog();
                break;
            case history:
                showHistoryPopup();
                break;
            case logout:
                logout();
                break;
            default:
                break;
        }
        return false;
    }

    private TextView createDateView(Date date) {
        TextView dateView = new TextView(SettingsActivity.this);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, 0, 20, 0);
        dateView.setLayoutParams(llp);
        dateView.setTextAppearance(SettingsActivity.this, R.style.DateView);
        dateView.setText(format.format(date));
        return dateView;
    }

    private TextView createScoreView(int score) {
        TextView scoreView = new TextView(SettingsActivity.this);
        scoreView.setText(getString(R.string.score) + " " + String.valueOf(score));
        scoreView.setTextAppearance(SettingsActivity.this, R.style.ScoreView);
        return scoreView;
    }

    private TextView createDurationView(long duration) {
        TextView durationView = new TextView(SettingsActivity.this);
        int seconds = (int) duration / 1000;
        int hours = seconds / 3600;
        int minutes = (seconds / 60) % 60;
        seconds = seconds % 60;
        String durationString = getString(R.string.duration) + " ";
        if (hours > 0) {
            durationString = hours + "h ";
        }
        if (minutes > 0) {
            durationString += minutes + "m ";
        }
        durationString += seconds + "s";
        durationView.setText(durationString);
        durationView.setTextAppearance(SettingsActivity.this, R.style.DurationView);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, 0, 20, 0);
        durationView.setLayoutParams(llp);
        return durationView;
    }

    private void showHistoryPopup() {
        historyView = getLayoutInflater().inflate(R.layout.history_view, null);

        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        final FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mFirebaseUser.getUid());

        // Attach a listener to read the data at our posts reference
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                LinearLayout list = (LinearLayout) historyView.findViewById(R.id.historyList);
                DataSnapshot statsRoot = dataSnapshot.child("stats");
                historyView.findViewById(R.id.loadingHistoryTextview).setVisibility(View.GONE);
                List<TripStats> allTrips = new ArrayList<>();
                int maxScore = -1;
                long totalDuration = 0;
                for (DataSnapshot entry : statsRoot.getChildren()) {
                    TripStats trip = entry.getValue(TripStats.class);
                    allTrips.add(trip);
                    if (maxScore < trip.score) {
                        maxScore = trip.score;
                    }
                    totalDuration += trip.tripDuration;
                }

                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                llp.setMargins(30, 30, 30, 0);
                LinearLayout summaryEntry = new LinearLayout(SettingsActivity.this);
                TextView v = new TextView(SettingsActivity.this);
                v.setText(getString(R.string.total_trips) + " " + allTrips.size() + " "
                        + getString(R.string.trip_unit) + " " + getString(R.string.max_score)
                        + maxScore + " " + getString(R.string.score_unit_display) + " "
                        + getString(R.string.total_duration) + " " + totalDuration / 1000
                        + " " + getString(R.string.duration_unit));
                v.setTextAppearance(SettingsActivity.this, R.style.HistorySummary);
                summaryEntry.addView(v);
                summaryEntry.setLayoutParams(llp);
                list.addView(summaryEntry);
                for (int i = allTrips.size() - 1; i >= 0; --i) {
                    TripStats stats = allTrips.get(i);
                    LinearLayout listEntry = new LinearLayout(SettingsActivity.this);
                    listEntry.setLayoutParams(llp);

                    Date date = new Date(stats.tripStartTimestampMillis);
                    TextView dateView = createDateView(date);
                    TextView scoreView = createScoreView(stats.score);
                    TextView durationView = createDurationView(stats.tripDuration);

                    listEntry.addView(dateView);
                    listEntry.addView(durationView);
                    listEntry.addView(scoreView);
                    listEntry.setOrientation(LinearLayout.HORIZONTAL);

                    list.addView(listEntry);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "The read failed: " + databaseError.getCode());
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.history_menu_title)
                .setView(historyView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_warning_title)
                .setMessage(R.string.logout_warning_text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Logout here.
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(SettingsActivity.this, LogInActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) { }})
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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

    private void showQuestionTypesMenu() {
        final List<String> availableTypes = new ArrayList();
        availableTypes.addAll(Constants.ALL_QUESTION_TYPES);
        Set<String> savedTypes = prefs.getStringSet(getString(R.string.question_types_key),
                new HashSet<String>());

        final boolean[] selectedBoolean = new boolean[availableTypes.size()];
        int i = 0;
        final String[] questionTypeLocalizedDisplayStrings = new String[availableTypes.size()];
        for (String type : availableTypes) {
            if (savedTypes.contains(type)) {
                selectedBoolean[i] = true;
            }
            QuestionAnswer.QuestionType typeEnum = QuestionAnswer.QuestionType.fromString(type);
            String displayString = getString(typeEnum.getResId());
            questionTypeLocalizedDisplayStrings[i] = displayString;
            ++i;
        }
        final List<String> selected = new ArrayList<>(savedTypes);
        final TextView selectQuestionTypeTextView = (TextView) findViewById(R.id.selectQuestionTypeValue);
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_question_type_label)
                .setMultiChoiceItems(questionTypeLocalizedDisplayStrings, selectedBoolean,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                String type = availableTypes.get(which);
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    selected.add(type);
                                } else if (selected.contains(type)) {
                                    if (selected.size() == 1) {
                                        selectedBoolean[which] = true;
                                        Toast.makeText(SettingsActivity.this, R.string.zero_question_type_warning,
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Else, if the item is already in the array, remove it
                                        selected.remove(type);
                                    }
                                }
                            }
                        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences prefs =
                                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putStringSet(getString(R.string.question_types_key), new HashSet(selected));
                        editor.commit();
                        selectQuestionTypeTextView.setText(getSelectedQuestionTypesMessage(selected.size()));
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
                    public void onDismiss(DialogInterface dialog) {}
                })
                .show();
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
        setupRecordNewToneMenu();
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
