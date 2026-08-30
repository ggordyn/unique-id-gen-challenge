package com.challenge.idgen.time;

public interface TimeSource {

    long currentTimeMillis();

    // Waits until the clock passes lastTimestamp. Shared by the sequence-overflow
    // wait and the bounded clock-rollback wait which makes both testable without real sleeps.
    default long waitForNextMillis(long lastTimestamp) {
        long now = currentTimeMillis();
        while (now <= lastTimestamp) {
            Thread.onSpinWait();
            now = currentTimeMillis();
        }
        return now;
    }
}
