package com.challenge.idgen.time;

import java.time.Clock;

public class SystemTimeSource implements TimeSource {

    private final Clock clock;

    public SystemTimeSource() {
        this(Clock.systemUTC());
    }

    public SystemTimeSource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public long currentTimeMillis() {
        return clock.millis();
    }
}
