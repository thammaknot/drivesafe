package com.knottycode.drivesafe;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by thammaknot on 1/21/17.
 */

public class Constants {
    public static final String SAFE_PHRASE = "ระนองระยองยะลา";

    public static final int TIMER_INTERVAL_MILLIS = 100;
    public static final int CHECKPOINT_GRACE_PERIOD_MILLIS = 5 * 1000;
    public static final int QUESTION_ANSWER_GRACE_PERIOD_MILLIS = 5 * 1000;
    public static final int DEFAULT_CHECKPOINT_FREQUENCY_SECONDS = 10;
    public static final AlertMode DEFAULT_ALERT_STYLE = AlertMode.SOUND;
    public static final int UNIT_SILENCE_DURATION_MILLIS = 500;

    public static final String DEFAULT_ALERT_SOUND = "alert/glitchy-tone.mp3";
    public static final String RECORDED_TONE_FILENAME = "recorded_tone.3gp";
    public static final float ALERT_VOLUME = 0.8f;
    public static final String ALARM_PATH_PREFIX = "alarm";
    public static final String QUESTION_PATH_PREFIX = "questions";

    public static final Set<String> allAlarmTones = new HashSet<String>();

    static {
        allAlarmTones.add("best_wake_up_sound.mp3");
        allAlarmTones.add("car_alarm.mp3");
        allAlarmTones.add("emergency_alert.mp3");
        allAlarmTones.add("fore_truck_siren.mp3");
        allAlarmTones.add("funny_alarm.mp3");
        allAlarmTones.add("pager_tone_112.mp3");
        allAlarmTones.add("rooster_alarm.mp3");
    }

    public static final String QUESTION_UTT_ID = "00000000";
    public static final String ANSWER_KEYWORD_UTT_ID = "00000001";
    public static final String ANSWER_UTT_ID = "00000002";
    public static final String SILENCE_UTT_ID = "00000003";
    public static final String CORRECT_KEYWORD_UTT_ID = "00000004";

    public static final String ANSWER_KEYWORD = "เฉลย";
    public static final String CORRECT_KEYWORD = "ถูกต้องนะค้า";
    public static final Set<String> SKIP_WORDS = new HashSet<String>();

    static {
        SKIP_WORDS.add("ผ่าน");
        SKIP_WORDS.add("ข้าม");
        SKIP_WORDS.add("ไม่รู้");
    }

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
            return SOUND;
        }

        static AlertMode fromCode(int c) {
            for (AlertMode m : AlertMode.values()) {
                if (m.code == c) {
                    return m;
                }
            }
            return SOUND;
        }

        int getCode() { return code; }

        String getDisplayString(Context context) {
            return context.getString(displayString);
        }
    }
}
