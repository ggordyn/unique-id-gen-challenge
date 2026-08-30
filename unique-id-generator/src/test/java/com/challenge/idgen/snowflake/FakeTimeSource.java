package com.challenge.idgen.snowflake;

import com.challenge.idgen.time.TimeSource;

class FakeTimeSource implements TimeSource {

    private long currentMillis;

    FakeTimeSource(long initialMillis) {
        this.currentMillis = initialMillis;
    }

    @Override
    public long currentTimeMillis() {
        return currentMillis;
    }

    void advanceTo(long millis) {
        currentMillis = millis;
    }

    // Simulates the clock ticking forward exactly 1ms the moment the generator asks to wait, 
    // so tests never spin or sleep.
    @Override
    public long waitForNextMillis(long lastTimestamp) {
        currentMillis = lastTimestamp + 1;
        return currentMillis;
    }
}
