package com.challenge.idgen.config;

import com.challenge.idgen.exception.InvalidConfigurationException;

import java.time.Instant;

// 64-bit Snowflike-like ID: 1 unused sign bit + 41-bit timestamp (fixed) + datacenterIdBits + workerIdBits
// + sequenceBits. Machine count is configurable; fixed timestamp width (~69-year usable window)
public record SnowflakeConfig(
        long epochMillis,
        int datacenterIdBits,
        int workerIdBits,
        int sequenceBits,
        long maxBackwardDriftMillis) {

    private static final int TOTAL_NON_TIMESTAMP_BITS = 22;

    private static final long DEFAULT_EPOCH_MILLIS = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
    private static final int DEFAULT_DATACENTER_ID_BITS = 5;
    private static final int DEFAULT_WORKER_ID_BITS = 5;
    private static final int DEFAULT_SEQUENCE_BITS = 12;
    private static final long DEFAULT_MAX_BACKWARD_DRIFT_MILLIS = 10;

    public SnowflakeConfig {
        if (datacenterIdBits <= 0 || workerIdBits <= 0 || sequenceBits <= 0) {
            throw new InvalidConfigurationException(
                    "datacenterIdBits, workerIdBits and sequenceBits must each be > 0, got: "
                            + datacenterIdBits + ", " + workerIdBits + ", " + sequenceBits);
        }
        int totalBits = datacenterIdBits + workerIdBits + sequenceBits;
        if (totalBits != TOTAL_NON_TIMESTAMP_BITS) {
            throw new InvalidConfigurationException(
                    "datacenterIdBits + workerIdBits + sequenceBits must equal "
                            + TOTAL_NON_TIMESTAMP_BITS + ", got: " + totalBits);
        }
        if (epochMillis > System.currentTimeMillis()) {
            throw new InvalidConfigurationException("epochMillis must not be in the future: " + epochMillis);
        }
        if (maxBackwardDriftMillis < 0) {
            throw new InvalidConfigurationException(
                    "maxBackwardDriftMillis must be >= 0, got: " + maxBackwardDriftMillis);
        }
    }

    public static SnowflakeConfig defaults() {
        return new SnowflakeConfig(
                DEFAULT_EPOCH_MILLIS,
                DEFAULT_DATACENTER_ID_BITS,
                DEFAULT_WORKER_ID_BITS,
                DEFAULT_SEQUENCE_BITS,
                DEFAULT_MAX_BACKWARD_DRIFT_MILLIS);
    }

    public int maxDatacenterId() {
        return (1 << datacenterIdBits) - 1;
    }

    public int maxWorkerId() {
        return (1 << workerIdBits) - 1;
    }

    public int maxSequence() {
        return (1 << sequenceBits) - 1;
    }

    public int workerIdShift() {
        return sequenceBits;
    }

    public int datacenterIdShift() {
        return sequenceBits + workerIdBits;
    }

    public int timestampShift() {
        return sequenceBits + workerIdBits + datacenterIdBits;
    }
}
