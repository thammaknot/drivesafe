package com.knottycode.drivesafe;

import android.content.Context;

import java.io.Serializable;
import java.util.ArrayList;
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
    private int score = 0;
    private int numCorrect = 0;
    private int numIncorrect = 0;
    private int numSkip = 0;
    private int numNoResponse = 0;

    private static final int MIN_FREQUENCY_MILLIS = 30 * 1000;
    private static final int MAX_FREQUENCY_MILLIS = 5 * 60 * 1000;
    private static final int RESPONSE_TIME_CUTOFF_MILLIS = 2 * 1000;
    private static final int FREQUENCY_INCREMENT_MILLIS = 15 * 1000;

    public CheckpointManager(long initFrequencyMillis, long startTime, Context context) {
        this.initFrequencyMillis = initFrequencyMillis;
        this.nextFrequencyMillis = initFrequencyMillis;
        this.driveModeStartTime = startTime;
    }

    public static CheckpointManager getInstance(long initFrequencyMillis,
                                                long startTime, Context context) {
        if (singletonInstance == null) {
            singletonInstance = new CheckpointManager(initFrequencyMillis, startTime, context);
        }
        return singletonInstance;
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
}
