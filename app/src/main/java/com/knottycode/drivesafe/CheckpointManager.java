package com.knottycode.drivesafe;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thammaknot on 2/6/17.
 */

public class CheckpointManager {
    private static String TAG = "CheckpointManager##";

    List<Long> responseTimeMillis = new ArrayList<>();
    private long initFrequencyMillis;
    private long nextFrequencyMillis;
    private boolean adaptive;

    private static final int MIN_FREQUENCY_MILLIS = 30 * 1000;
    private static final int MAX_FREQUENCY_MILLIS = 5 * 60 * 1000;
    private static final int RESPONSE_TIME_CUTOFF_MILLIS = 2 * 1000;
    private static final int FREQUENCY_INCREMENT_MILLIS = 15 * 1000;

    public CheckpointManager(long initFrequencyMillis, boolean adaptive) {
        this.initFrequencyMillis = initFrequencyMillis;
        this.nextFrequencyMillis = initFrequencyMillis;
        this.adaptive = adaptive;
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
