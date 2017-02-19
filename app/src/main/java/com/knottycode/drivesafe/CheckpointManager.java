package com.knottycode.drivesafe;

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
    private boolean adaptive;
    private String text;
    private long driveModeStartTime = -1;

    private static final int MIN_FREQUENCY_MILLIS = 30 * 1000;
    private static final int MAX_FREQUENCY_MILLIS = 5 * 60 * 1000;
    private static final int RESPONSE_TIME_CUTOFF_MILLIS = 2 * 1000;
    private static final int FREQUENCY_INCREMENT_MILLIS = 15 * 1000;

    public CheckpointManager(long initFrequencyMillis, boolean adaptive, long startTime) {
        this.initFrequencyMillis = initFrequencyMillis;
        this.nextFrequencyMillis = initFrequencyMillis;
        this.adaptive = adaptive;
        this.driveModeStartTime = startTime;
    }

    public static CheckpointManager getInstance(long initFrequencyMillis, boolean adaptive,
                                                long startTime) {
        if (singletonInstance == null) {
            singletonInstance = new CheckpointManager(initFrequencyMillis, adaptive, startTime);
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
        if (adaptive) {
            calculateNextFrequency();
        }
    }
    
    public long getNextFrequencyMillis() {
        return nextFrequencyMillis;
    }
}
