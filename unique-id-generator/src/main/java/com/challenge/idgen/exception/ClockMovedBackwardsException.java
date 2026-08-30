package com.challenge.idgen.exception;

public class ClockMovedBackwardsException extends RuntimeException {

    private final long driftMillis;

    public ClockMovedBackwardsException(long driftMillis) {
        super("Clock moved backwards by " + driftMillis + "ms");
        this.driftMillis = driftMillis;
    }

    public long driftMillis() {
        return driftMillis;
    }
}
