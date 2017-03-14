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

import java.util.ArrayList;
import java.util.List;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import static com.knottycode.drivesafe.R.id.checkpointFrequencyDisplay;
import static com.knottycode.drivesafe.R.id.settingsButton;

public class MainActivity extends Activity {

    TextView checkpointFrequencyTextView;
    ImageButton driveButton;
    ImageButton settingsButton;

    protected long checkpointFrequencyMillis;
    protected List<String> availableAlarmTones;
    private SharedPreferences preferences;
    private static final String SHOWCASE_ID = "tutorial_showcase";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    private void checkAndShowTutorial() {
        if (preferences.getBoolean(getString(R.string.tutorial_complete_key), false)) {
            return;
        }

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(300);  // millis

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, SHOWCASE_ID);
        sequence.setConfig(config);
        String gotIt = getString(R.string.got_it);
        sequence.addSequenceItem(checkpointFrequencyTextView,
                getString(R.string.checkpoint_frequency_tutorial), gotIt);
        sequence.addSequenceItem(settingsButton,
                getString(R.string.settings_button_tutorial), gotIt);
        sequence.addSequenceItem(driveButton,
                getString(R.string.drive_button_tutorial), gotIt);

        sequence.start();
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
                        Constants.allAlarmTones));
    }

    private void displayPreferences() {
        int checkpointFreqSeconds = (int) (checkpointFrequencyMillis / 1000);
        int minutes = checkpointFreqSeconds / 60;
        int seconds = checkpointFreqSeconds % 60;
        checkpointFrequencyTextView.setText(String.format("%02d:%02d", minutes, seconds));
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

    @Override
    public void onBackPressed() {
        // Exit the app.
        finish();
    }
}
