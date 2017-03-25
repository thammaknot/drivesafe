package com.knottycode.drivesafe;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Created by thammaknot on 1/21/17.
 */

public class Constants {
    public static final Random RANDOM = new Random();
    public static final String SAFE_PHRASE = "ระนองระยองยะลา";

    public static final int TIMER_INTERVAL_MILLIS = 100;
    public static final int CHECKPOINT_GRACE_PERIOD_MILLIS = 5 * 1000;
    public static final int QUESTION_ANSWER_GRACE_PERIOD_MILLIS = 20 * 1000;
    public static final int QUESTION_ANSWER_MAX_GRACE_PERIOD_MILLIS = 25 * 1000;
    public static final int GRACE_PERIOD_EXTENSION_SECONDS = 20;
    public static final int MIN_TIME_TO_RESTART_ASR_MILLIS = 4 * 1000;
    public static final int DEFAULT_CHECKPOINT_FREQUENCY_SECONDS = 60;
    public static final AlertMode DEFAULT_ALERT_STYLE = AlertMode.SOUND;
    public static final int UNIT_SILENCE_DURATION_MILLIS = 500;
    public static final Locale THAI_LOCALE = new Locale("th", "TH");
    public static final int MAX_ASR_RESULTS = 10;

    public static final String DEFAULT_ALERT_SOUND = "alert/glitchy-tone.mp3";
    public static final String RECORDED_TONE_FILENAME = "recorded_tone.3gp";
    public static final float ALERT_VOLUME = 0.8f;
    public static final String ALARM_PATH_PREFIX = "alarm";
    public static final String QUESTION_PATH_PREFIX = "questions";

    public static final String MATH_QUESITON_ID = "MX";

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
    public static final String TRY_AGAIN_UTT_ID = "00000005";
    public static final String EXTEND_GRACE_PERIOD_UTT_ID = "00000006";
    public static final String PREAMBLE_UTT_ID = "00000007";
    public static final String SCORE_UTT_ID = "00000008";

    public static final String ANSWER_KEYWORD = "เฉลย";
    public static final List<String> CORRECT_KEYWORDS = new ArrayList<>();
    public static final List<String> TRY_AGAIN_KEYWORDS = new ArrayList<>();
    public static final List<String> EXTEND_GRACE_PERIOD_KEYWORDS = new ArrayList<>();
    public static final Set<String> SKIP_WORDS = new HashSet<String>();
    public static final Set<String> EXTENSION_WORDS = new HashSet<>();

    static {
        SKIP_WORDS.add("ผ่าน");
        SKIP_WORDS.add("ข้าม");
        SKIP_WORDS.add("ไม่รู้");
        SKIP_WORDS.add("ยอมแพ้");

        CORRECT_KEYWORDS.add("ถูกต้องนะค้า");
        CORRECT_KEYWORDS.add("ยอดเยี่ยมไปเลย");
        CORRECT_KEYWORDS.add("เก่งมากนะค้า");

        TRY_AGAIN_KEYWORDS.add("ลองอีกทีนะ");
        TRY_AGAIN_KEYWORDS.add("ยังไม่ถูกนะค้า");
        TRY_AGAIN_KEYWORDS.add("ให้โอกาสอีกทีค่ะ");
        TRY_AGAIN_KEYWORDS.add("ให้ลองอีกทีนะ");

        EXTEND_GRACE_PERIOD_KEYWORDS.add("ต่อเวลาให้");
        EXTEND_GRACE_PERIOD_KEYWORDS.add("เพิ่มเวลาให้");
        EXTEND_GRACE_PERIOD_KEYWORDS.add("ให้เวลาอีก");

        EXTENSION_WORDS.add("ต่อเวลา");
        EXTENSION_WORDS.add("ขอเวลาอีกหน่อย");
        EXTENSION_WORDS.add("ขอเวลาอีกแป๊บ");
        EXTENSION_WORDS.add("ขอเวลาอีกเดี๋ยว");
        EXTENSION_WORDS.add("ขอเวลาอีกหน่อยได้ไหม");
        EXTENSION_WORDS.add("ขอเวลาเพิ่ม");
        EXTENSION_WORDS.add("อีกแป๊บนึง");
    }

    public static final Set<String> ALL_QUESTION_TYPES = new HashSet<String>();
    public static final int NUM_MATH_QUESTIONS = 50;
    public static final String MATH_ADDITION = "บวก";
    public static final String MATH_MULTIPLICATION = "คูณ";
    public static final String MATH_QUESTION_ENDING = "เท่ากับเท่าไหร่";

    static {
        ALL_QUESTION_TYPES.add("funny");
        ALL_QUESTION_TYPES.add("math");
        ALL_QUESTION_TYPES.add("trivia");
        ALL_QUESTION_TYPES.add("riddles");
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
