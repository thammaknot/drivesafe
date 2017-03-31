package com.knottycode.drivesafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import static com.knottycode.drivesafe.R.id.checkpointFrequencyDisplay;

public class MainActivity extends Activity {

    TextView checkpointFrequencyTextView;
    ImageButton driveButton;
    ImageButton settingsButton;

    protected long checkpointFrequencyMillis;
    protected List<String> availableAlarmTones;
    private SharedPreferences preferences;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not logged in, launch the Log In activity
            loadLogInView();
            return;
        }

        {
            // Get the shared preferences
            preferences =
                    getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);

            // Check if onboarding_complete is false
            if (!preferences.getBoolean(getString(R.string.onboarding_complete_key), false)) {
                // Start the onboarding Activity
                Intent onboarding = new Intent(this, IntroActivity.class);
                startActivity(onboarding);
                // Close the main Activity
                finish();
                return;
            }
        }

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        checkpointFrequencyTextView = (TextView) findViewById(checkpointFrequencyDisplay);
        // checkpointFrequencyTextView.setVisibility(View.INVISIBLE);
        TextView checkpointFrequencyLabel = (TextView) findViewById(R.id.checkpointFrequencyLabel);
        // checkpointFrequencyLabel.setVisibility(View.INVISIBLE);
        driveButton = (ImageButton) findViewById(R.id.driveButton);
        settingsButton = (ImageButton) findViewById(R.id.settingsButton);

        checkAndShowTutorial();
    }

    private void loadLogInView() {
        Intent intent = new Intent(this, LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkAndShowTutorial() {
        if (preferences.getBoolean(getString(R.string.tutorial_complete_key), false)) {
            return;
        }

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(300);  // millis

        int tutorialId =
                preferences.getInt(getString(R.string.tutorial_id_key), 0);
        MaterialShowcaseSequence sequence =
                new MaterialShowcaseSequence(this, String.valueOf(tutorialId));
        sequence.setConfig(config);
        String gotIt = getString(R.string.got_it);
        sequence.addSequenceItem(checkpointFrequencyTextView,
                getString(R.string.checkpoint_frequency_tutorial), gotIt);
        sequence.addSequenceItem(settingsButton,
                getString(R.string.settings_button_tutorial), gotIt);
        sequence.addSequenceItem(driveButton,
                getString(R.string.drive_button_tutorial), gotIt);
        sequence.start();
        preferences.edit()
                .putBoolean(getString(R.string.tutorial_complete_key), true).apply();
        preferences.edit()
                .putInt(getString(R.string.tutorial_id_key), tutorialId + 1).apply();
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
        checkpointFrequencyMillis =
                prefs.getInt(getString(R.string.checkpoint_frequency_key),
                        Constants.DEFAULT_CHECKPOINT_FREQUENCY_SECONDS) * 1000;
        availableAlarmTones =
                new ArrayList<String>(prefs.getStringSet(getString(R.string.alarm_tones_key),
                        Constants.ALL_ALARM_TONES));
    }

    private void displayPreferences() {
        int checkpointFreqSeconds = (int) (checkpointFrequencyMillis / 1000);
        checkpointFrequencyTextView.setText(
                SettingsActivity.getNaturalLanguageText(checkpointFreqSeconds, this));
    }

    public void enterDriveMode(View view) {
        Intent intent = new Intent(this, DriveModeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    public void enterSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        finish();
    }

    public void onClickInfo(View view) {
        preferences.edit()
                .putBoolean(getString(R.string.tutorial_complete_key), false).apply();
        // Start the onboarding Activity
        Intent onboarding = new Intent(this, IntroActivity.class);
        startActivity(onboarding);
        // Close the main Activity
        finish();
        return;
    }

    private String formatTime(int seconds) {
        return String.format(SettingsActivity.getNaturalLanguageText(seconds, this));
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

    @Override
    public void onBackPressed() {
        // Exit the app.
        finish();
    }
}
