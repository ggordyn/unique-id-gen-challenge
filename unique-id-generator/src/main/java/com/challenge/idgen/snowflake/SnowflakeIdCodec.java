package com.challenge.idgen.snowflake;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.exception.InvalidConfigurationException;
import com.challenge.idgen.model.DecodedId;

// 64-bit long ID with four fields (defaults as follows):
// [ sign (1, unused) | timestamp (41) | datacenter (5) | worker (5) | sequence (12) ]
// To encode, each field is shifted left into its slot and OR'd together. To decode, do the same in reverse.
// The sign bit is never touched (for now).
public final class SnowflakeIdCodec {

    private SnowflakeIdCodec() {
    }

    public static long encode(long timestamp, int datacenterId, int workerId, int sequence, SnowflakeConfig config) {
        long elapsed = timestamp - config.epochMillis();
        if (elapsed < 0) {
            throw new InvalidConfigurationException("timestamp must not be before the epoch: " + timestamp);
        }
        checkInRange("datacenterId", datacenterId, config.maxDatacenterId());
        checkInRange("workerId", workerId, config.maxWorkerId());
        checkInRange("sequence", sequence, config.maxSequence());

        // Cast to long before shifting to run as 64-bit math, otherwise the shift is done as 32-bit math and the result is truncated to 32 bits.
        return (elapsed << config.timestampShift())
                | ((long) datacenterId << config.datacenterIdShift())
                | ((long) workerId << config.workerIdShift())
                | sequence;
    }

    // Out-of-range fields would otherwise bleed into adjacent bit fields and silently corrupt the ID.
    private static void checkInRange(String name, int value, int max) {
        if (value < 0 || value > max) {
            throw new InvalidConfigurationException(name + " must be in [0, " + max + "], got: " + value);
        }
    }

    public static DecodedId decode(long id, SnowflakeConfig config) {
        // (int): The mask already guarantees the result fits in an int
        int sequence = (int) (id & config.maxSequence());
        int workerId = (int) ((id >> config.workerIdShift()) & config.maxWorkerId());
        int datacenterId = (int) ((id >> config.datacenterIdShift()) & config.maxDatacenterId());
        long elapsed = id >> config.timestampShift();
        long timestamp = elapsed + config.epochMillis();
        return new DecodedId(id, timestamp, datacenterId, workerId, sequence);
    }
}
