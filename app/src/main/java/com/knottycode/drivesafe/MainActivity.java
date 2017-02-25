package com.knottycode.drivesafe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.google.android.gms.internal.zzs.TAG;

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

        checkpointFrequencyTextView = (TextView) findViewById(R.id.checkpointFrequencyDisplay);
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
                new ArrayList<String>(prefs.getStringSet(getString(R.string.alarm_tones_key), new HashSet()));
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
            findViewById(R.id.icon1).setBackground(getResources().getDrawable(R.drawable.icon1));
        } else {
            findViewById(R.id.icon1).setBackground(getResources().getDrawable(R.drawable.icon1_disabled));
        }
        switch (alertMode) {
            case SCREEN:
                findViewById(R.id.icon2).setVisibility(View.VISIBLE);
                findViewById(R.id.icon2).setBackground(getResources().getDrawable(R.drawable.icon2));
                break;
            case VIBRATE:
                findViewById(R.id.icon2).setVisibility(View.VISIBLE);
                findViewById(R.id.icon2).setBackground(getResources().getDrawable(R.drawable.icon2_disabled));
                break;
            case SOUND:
                findViewById(R.id.icon2).setVisibility(View.INVISIBLE);
                break;
            default:
        }
        if (adaptiveLoudness) {
            findViewById(R.id.icon3).setBackground(getResources().getDrawable(R.drawable.icon3));
        } else {
            findViewById(R.id.icon3).setBackground(getResources().getDrawable(R.drawable.icon3_disabled));
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

    @Override
    public void onBackPressed() {
        // Exit the app.
        finish();
    }
}
