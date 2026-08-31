package com.challenge.idgen.snowflake;

import com.challenge.idgen.IdGenerator;
import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.exception.ClockMovedBackwardsException;
import com.challenge.idgen.time.TimeSource;

public class SnowflakeIdGenerator implements IdGenerator {

    private final SnowflakeConfig config;
    private final WorkerId workerId;
    private final TimeSource timeSource;

    private long lastTimestamp = -1;
    private int sequence = 0;

    public SnowflakeIdGenerator(SnowflakeConfig config, WorkerId workerId, TimeSource timeSource) {
        workerId.validate(config);
        this.config = config;
        this.workerId = workerId;
        this.timeSource = timeSource;
    }

    @Override
    public synchronized long nextId() {
        long now = timeSource.currentTimeMillis();

        if (now < lastTimestamp) {
            // Clock stepped backwards (NTP correction, VM pause). A small drift doesn't need to
            // block: reuse lastTimestamp and extend the sequence below. We only actually wait if that 
            // runs out of sequence room.
            // A large drift is likely a real clock/host problem, so fail instead of blocking.
            long drift = lastTimestamp - now;
            if (drift > config.maxBackwardDriftMillis()) {
                throw new ClockMovedBackwardsException(drift);
            }
            now = lastTimestamp;
        }

        if (now == lastTimestamp) {
            // Same millisecond as the last call (or a small backward drift treated as such):
            // +1 counter. If counter > max IDs, wait for the next ms and reset.
            sequence++;
            if (sequence > config.maxSequence()) {
                now = timeSource.waitForNextMillis(lastTimestamp);
                sequence = 0;
            }
        } else {
            // A later millisecond than last time: start the counter over.
            sequence = 0;
        }

        lastTimestamp = now;
        return SnowflakeIdCodec.encode(now, workerId.datacenterId(), workerId.workerId(), sequence, config);
    }
}
