package com.knottycode.drivesafe;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private List<QuestionAnswer> questions;
    private int nextQuestionIndex = 0;

    private static final int MIN_FREQUENCY_MILLIS = 30 * 1000;
    private static final int MAX_FREQUENCY_MILLIS = 5 * 60 * 1000;
    private static final int RESPONSE_TIME_CUTOFF_MILLIS = 2 * 1000;
    private static final int FREQUENCY_INCREMENT_MILLIS = 15 * 1000;

    public CheckpointManager(long initFrequencyMillis, long startTime, Context context) {
        this.initFrequencyMillis = initFrequencyMillis;
        this.nextFrequencyMillis = initFrequencyMillis;
        this.driveModeStartTime = startTime;
        loadQuestions(context);
    }

    public static CheckpointManager getInstance(long initFrequencyMillis,
                                                long startTime, Context context) {
        if (singletonInstance == null) {
            singletonInstance = new CheckpointManager(initFrequencyMillis, startTime, context);
        }
        return singletonInstance;
    }

    private void loadQuestions(Context context) {
        AssetManager am = context.getAssets();
        InputStream is;
        questions = new ArrayList<QuestionAnswer>();
        try {
            is = am.open(Constants.QUESTION_PATH_PREFIX + "/fun_questions.txt");
        } catch (IOException io) {
            // Leave the set empty.
            return;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = r.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != 3) {
                    Log.w(TAG, "Malformed line in question file: " + line);
                    continue;
                }
                questions.add(
                        new QuestionAnswer(tokens[0], tokens[1],
                                QuestionAnswer.QuestionType.FUNNY, tokens[2]));
            }
            Collections.shuffle(questions);
        } catch (IOException io) {
            Log.e(TAG, "Exception while reading question file.");
            io.printStackTrace();
        }
    }

    public QuestionAnswer getNextQuestion() {
        int i = nextQuestionIndex % questions.size();
        ++nextQuestionIndex;
        return questions.get(i);
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
}
