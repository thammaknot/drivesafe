package com.knottycode.drivesafe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button driveButton;
    Button settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        driveButton = (Button) findViewById(R.id.driveButton);
        settingsButton = (Button) findViewById(R.id.settingsButton);
    }

    public void enterDriveMode(View view) {
        Intent intent = new Intent(this, DriveModeActivity.class);
        startActivity(intent);
    }

    public void enterSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
