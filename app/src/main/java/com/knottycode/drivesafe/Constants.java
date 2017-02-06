package com.knottycode.drivesafe;

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
        SCREEN(1, "Screen only"),
        VIBRATE(2, "Vibration"),
        SOUND(3, "Sound");

        private int code;
        private String displayString;

        AlertMode(int code, String s) {
            this.code = code;
            displayString = s;
        }

        static AlertMode fromString(String s) {
            for (AlertMode m : AlertMode.values()) {
                if (m.displayString.equals(s)) {
                    return m;
                }
            }
            return SCREEN;
        }

        int getCode() {
            return code;
        }

        String getDisplayString() {
            return displayString;
        }
    }
}