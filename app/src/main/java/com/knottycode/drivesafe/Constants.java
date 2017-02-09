package com.knottycode.drivesafe;

import android.content.Context;

/**
 * Created by thammaknot on 1/21/17.
 */

public class Constants {
    public static final int TIMER_INTERVAL_MILLIS = 100;
    public static final int CHECKPOINT_GRACE_PERIOD_MILLIS = 5 * 1000;
    public static final int DEFAULT_CHECKPOINT_FREQUENCY_SECONDS = 10;
    public static final AlertMode DEFAULT_ALERT_STYLE = AlertMode.SCREEN;

    public static final String DEFAULT_ALERT_SOUND = "alert/glitchy-tone.mp3";
    public static final float ALERT_VOLUME = 0.8f;

    public static final long[] VIBRATION_PATTERN = {0, 700, 100, 400};

    public enum AlertMode {
        SCREEN(1, R.string.setting_checkpoint_alert_style_screen),
        VIBRATE(2, R.string.setting_checkpoint_alert_style_vibrate),
        SOUND(3, R.string.setting_checkpoint_alert_style_sound);

        private int code;
        private int displayString;

        AlertMode(int code, int s) {
            this.code = code;
            displayString = s;
        }

        static AlertMode fromString(Context context, String s) {
            for (AlertMode m : AlertMode.values()) {
                if (context.getString(m.displayString).equals(s)) {
                    return m;
                }
            }
            return SCREEN;
        }

        String getDisplayString(Context context) {
            return context.getString(displayString);
        }
    }
}
