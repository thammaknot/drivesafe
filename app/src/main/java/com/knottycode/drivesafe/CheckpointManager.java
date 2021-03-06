package com.knottycode.drivesafe;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.knottycode.drivesafe.QuestionAnswer.QuestionType;

/**
 * Created by thammaknot on 2/6/17.
 */

public class CheckpointManager implements Serializable {
    private static String TAG = "CheckpointManager##";

    private static CheckpointManager singletonInstance = null;

    List<Long> responseTimeMillis = new ArrayList<>();
    private long initFrequencyMillis;
    private long nextFrequencyMillis;
    private long driveModeStartTime = -1;

    private int score = 0;
    private int numCorrect = 0;
    private int numIncorrect = 0;
    private int numSkip = 0;
    private int numNoResponse = 0;

    private List<String> tipList;

    private List<QuestionAnswer> questions;
    private int nextQuestionIndex = 0;
    private boolean volumeAdjusted = false;

    private static final int MIN_FREQUENCY_MILLIS = 30 * 1000;
    private static final int MAX_FREQUENCY_MILLIS = 5 * 60 * 1000;
    private static final int RESPONSE_TIME_CUTOFF_MILLIS = 2 * 1000;
    private static final int FREQUENCY_INCREMENT_MILLIS = 15 * 1000;

    private FirebaseUser firebaseUser;

    public CheckpointManager(long initFrequencyMillis, long startTime, Context context) {
        this.initFrequencyMillis = initFrequencyMillis;
        this.nextFrequencyMillis = initFrequencyMillis;
        this.driveModeStartTime = startTime;
        loadTips(context);
        loadQuestions(context);
    }

    public static CheckpointManager getInstance(long initFrequencyMillis,
                                                long startTime, Context context) {
        if (singletonInstance == null) {
            singletonInstance = new CheckpointManager(initFrequencyMillis, startTime, context);
        }
        return singletonInstance;
    }

    private void loadTips(Context context) {
        InputStream is = null;
        AssetManager am = context.getAssets();
        try {
            is = am.open(Constants.TIP_FILE_PATH);
        } catch (IOException io) {
            Toast.makeText(context, "Unable to load tips", Toast.LENGTH_SHORT).show();
            io.printStackTrace();
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        tipList = new ArrayList<>();
        try {
            while ((line = r.readLine()) != null) {
                tipList.add(line);
            }
        } catch (IOException io) {
            Log.e(TAG, "Exception while reading question file.");
            io.printStackTrace();
        }
    }

    private void loadQuestions(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(context.getString(R.string.preference_file_key),
                        MODE_PRIVATE);
        Set<String> qTypeStrings =
                preferences.getStringSet(context.getString(R.string.question_types_key),
                        Constants.ALL_QUESTION_TYPES);
        Set<QuestionType> qTypes = new HashSet<>();
        for (String t : qTypeStrings) {
            qTypes.add(QuestionType.fromString(t));
        }

        AssetManager am = context.getAssets();
        InputStream is;
        questions = new ArrayList<QuestionAnswer>();

        for (QuestionType type : qTypes) {
            if (type == QuestionType.MATH) {
                for (int i = 0; i < Constants.NUM_MATH_QUESTIONS; ++i) {
                    questions.add(new QuestionAnswer(Constants.MATH_QUESITON_ID,
                            "", "", QuestionType.MATH, ""));
                }
                continue;
            }
            try {
                is = am.open(Constants.QUESTION_PATH_PREFIX + "/" + type.getFilename() + ".txt");
            } catch (IOException io) {
                // Skip this type.
                continue;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String line;
            int count = 0;
            try {
                while ((line = r.readLine()) != null) {
                    String[] tokens = line.split("\t");
                    if (tokens.length != 4) {
                        Log.w(TAG, "Malformed line in question file: " + line);
                        continue;
                    }
                    questions.add(
                            new QuestionAnswer(tokens[0], tokens[1], tokens[2],
                                    type, tokens[3]));
                    ++count;
                }
            } catch (IOException io) {
                Log.e(TAG, "Exception while reading question file.");
                io.printStackTrace();
            }
            Log.d(TAG, "##### Loaded " + count + " questions from " + type.getFilename());
        }
        Collections.shuffle(questions);
    }

    public QuestionAnswer getNextQuestion() {
        int i = nextQuestionIndex % questions.size();
        ++nextQuestionIndex;
        return questions.get(i);
    }

    public List<String> getTipList() {
        return tipList;
    }

    public static void invalidateSingletonInstance() {
        singletonInstance = null;
    }

    public long getDriveModeStartTime() {
        return driveModeStartTime;
    }

    private long calculateNextFrequency() {
        long lastResponseTime = responseTimeMillis.get(responseTimeMillis.size() - 1);
        if (lastResponseTime > RESPONSE_TIME_CUTOFF_MILLIS) {
            nextFrequencyMillis -= FREQUENCY_INCREMENT_MILLIS;
            nextFrequencyMillis = Math.max(nextFrequencyMillis, MIN_FREQUENCY_MILLIS);
        } else {
            nextFrequencyMillis += FREQUENCY_INCREMENT_MILLIS;
            nextFrequencyMillis = Math.min(nextFrequencyMillis, MAX_FREQUENCY_MILLIS);
        }
        return nextFrequencyMillis;
    }

    public void addResponseTime(long t) {
        responseTimeMillis.add(t);
    }
    
    public long getNextFrequencyMillis() {
        return nextFrequencyMillis;
    }

    public int getScore() {
        return score;
    }

    public enum ScoreMode {
        CORRECT, INCORRECT, SKIP, NO_RESPONSE;
    }

    public void updateScore(ScoreMode mode) {
        switch (mode) {
            case CORRECT:
                ++numCorrect;
                break;
            case INCORRECT:
                ++numIncorrect;
                break;
            case SKIP:
                ++numSkip;
                break;
            case NO_RESPONSE:
                ++numNoResponse;
                break;
            default:
        }
        score = numCorrect;
    }

    public void resetScores() {
        score = 0;
        numCorrect = 0;
        numIncorrect = 0;
        numSkip = 0;
        numNoResponse = 0;
    }

    public String getAllScores() {
        return "" + numCorrect + "|" + numIncorrect + "|" + numSkip + "|" + numNoResponse;
    }

    public void setVolumeAdjusted(boolean b) {
        volumeAdjusted = b;
    }

    public boolean getVolumeAdjusted() {
        return volumeAdjusted;
    }

    public void setUser(FirebaseUser user) {
        this.firebaseUser = user;
    }

    public FirebaseUser getUser() {
        return firebaseUser;
    }
}
