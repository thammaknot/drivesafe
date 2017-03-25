package com.knottycode.drivesafe;

/**
 * Created by thammaknot on 3/25/17.
 */

public class TripStats {

    public long tripDuration;
    public int score;
    public long tripStartTimestampMillis;

    public TripStats() {}

    public TripStats(long tripDuration, int score, long tripStartTimestampMillis) {
        this.tripDuration = tripDuration;
        this.score = score;
        this.tripStartTimestampMillis = tripStartTimestampMillis;
    }
}
