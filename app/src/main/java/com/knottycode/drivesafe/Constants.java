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
    public static final int QUESTION_ANSWER_GRACE_PERIOD_MILLIS = 25 * 1000;
    public static final int QUESTION_ANSWER_MAX_GRACE_PERIOD_MILLIS = 30 * 1000;
    public static final int GRACE_PERIOD_EXTENSION_SECONDS = 20;
    public static final int MIN_TIME_TO_RESTART_ASR_MILLIS = 4 * 1000;
    public static final int DEFAULT_CHECKPOINT_FREQUENCY_SECONDS = 30;
    public static final AlertMode DEFAULT_ALERT_STYLE = AlertMode.SOUND;
    public static final int UNIT_SILENCE_DURATION_MILLIS = 500;
    public static final int MAX_ASR_LISTENING_MILLIS = 6 * 1000;
    public static final int MAX_SONG_ASR_LISTENING_MILLIS = 12 * 1000;
    public static final int DRIVE_MODE_MAX_OVERTIME = 5 * 1000;
    public static final Locale THAI_LOCALE = new Locale("th", "TH");
    public static final int MAX_ASR_RESULTS = 10;

    public static final int MIN_TIME_TO_SPEAK_TIP_MILLIS = 6 * 1000;
    public static final int TIP_FREQUENCY_MILLIS = 20 * 1000;

    public static final String DEFAULT_ALERT_SOUND = "alert/glitchy-tone.mp3";
    public static final String RECORDED_TONE_FILENAME = "recorded_tone.3gp";
    public static final String BACKGROUND_MUSTIC_PATH_PREFIX = "background_music";
    public static final String QUESTION_TONE_FILENAME = "question_tone.mp3";
    public static final String SPLASH_MUSIC_FILENAME = "splash_music.mp3";
    public static final String TIP_FILE_PATH = "tips/tips.txt";
    public static final float ALERT_VOLUME = 0.8f;
    public static final String ALARM_PATH_PREFIX = "alarm";
    public static final String QUESTION_PATH_PREFIX = "questions";
    public static final String PLAY_STORE_PREFIX = "market://details?id=";
    public static final String PLAY_STORE_URL_PREFIX = "https://play.google.com/store/apps/details?id=";
    public static final String TTS_PACKAGE_NAME = "com.google.android.tts";

    public static final String MATH_QUESITON_ID = "MX";

    public static final Set<String> ALL_ALARM_TONES = new HashSet<String>();

    static {
        ALL_ALARM_TONES.add("best_wake_up_sound.mp3");
        ALL_ALARM_TONES.add("car_alarm.mp3");
        ALL_ALARM_TONES.add("emergency_alert.mp3");
        ALL_ALARM_TONES.add("fore_truck_siren.mp3");
        ALL_ALARM_TONES.add("funny_alarm.mp3");
        ALL_ALARM_TONES.add("pager_tone_112.mp3");
        ALL_ALARM_TONES.add("rooster_alarm.mp3");
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
    public static final String TIP_UTT_ID = "00000009";

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
        CORRECT_KEYWORDS.add("เก่งจัง");
        CORRECT_KEYWORDS.add("เก่งจังเลย");
        CORRECT_KEYWORDS.add("เก่งจังเลยค่ะ");
        CORRECT_KEYWORDS.add("สุดยอดไปเลย");
        CORRECT_KEYWORDS.add("สุดยอดไปเลยค่ะ");
        CORRECT_KEYWORDS.add("สุดยอดเลยค่ะ");
        CORRECT_KEYWORDS.add("เยี่ยมมาก");
        CORRECT_KEYWORDS.add("เยี่ยมมากค่ะ");
        CORRECT_KEYWORDS.add("เก่งจริงๆ");
        CORRECT_KEYWORDS.add("เก่งจริงๆ เลยนะ");
        CORRECT_KEYWORDS.add("เก่งจริงๆ เลยนะเนี่ย");

        TRY_AGAIN_KEYWORDS.add("ลองอีกทีนะ");
        TRY_AGAIN_KEYWORDS.add("ยังไม่ถูกนะค้า");
        TRY_AGAIN_KEYWORDS.add("ให้โอกาสอีกทีค่ะ");
        TRY_AGAIN_KEYWORDS.add("ให้ลองอีกทีนะ");
        TRY_AGAIN_KEYWORDS.add("ลองตอบอีกทีนะ");
        TRY_AGAIN_KEYWORDS.add("ลองตอบอีกทีนะคะ");
        TRY_AGAIN_KEYWORDS.add("ยังไม่ถูกค่ะ");
        TRY_AGAIN_KEYWORDS.add("คิดไม่ออกล่ะสิ, ให้ตอบอีกทีก็ได้");
        TRY_AGAIN_KEYWORDS.add("ให้ตอบอีกทีก็ได้");
        TRY_AGAIN_KEYWORDS.add("ให้ตอบอีกทีก็ได้นะ");
        TRY_AGAIN_KEYWORDS.add("ตอบอีกทีก็ได้นะ");
        TRY_AGAIN_KEYWORDS.add("ยังไม่ถูก, ให้ตอบอีกทีดีกว่า");
        TRY_AGAIN_KEYWORDS.add("ยังไม่ถูก, ให้ตอบอีกทีนะคะ");
        TRY_AGAIN_KEYWORDS.add("ยังไม่ถูกค่ะ, ตอบอีกทีนะคะ");
        TRY_AGAIN_KEYWORDS.add("ยังไม่ถูกนะ, ให้ตอบอีกทีดีกว่า");
        TRY_AGAIN_KEYWORDS.add("ยังไม่ถูกนะคะ, ให้ตอบอีกทีดีกว่า");

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
        ALL_QUESTION_TYPES.add("sports");
        ALL_QUESTION_TYPES.add("science");
        ALL_QUESTION_TYPES.add("songs");
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
