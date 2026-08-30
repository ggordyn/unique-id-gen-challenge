package com.challenge.idgen.time;

public interface TimeSource {

    long currentTimeMillis();

    // Busy-waits until the clock passes lastTimestamp. Shared by the sequence-overflow
    // wait and the bounded clock-rollback wait, and is what makes both testable without
    // real sleeps: fakes just control what currentTimeMillis() returns next.
    default long waitForNextMillis(long lastTimestamp) {
        long now = currentTimeMillis();
        while (now <= lastTimestamp) {
            Thread.onSpinWait();
            now = currentTimeMillis();
        }
        return now;
    }
}
