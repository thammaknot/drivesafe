package com.knottycode.drivesafe;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * Created by thammaknot on 2/18/17.
 */

public class AlarmModeActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm_mode);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }
}
