package com.challenge.idgen.snowflake;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.time.SystemTimeSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Uses the real SystemTimeSource to measure actual wall-clock throughput against the book's
// "10,000+ IDs/sec" requirement. Single-threaded on purpose, so thread-scheduling overhead
// doesn't confound the measurement -- SnowflakeIdGeneratorConcurrencyTest covers correctness
// under concurrent load, this covers raw speed.
class SnowflakeIdGeneratorThroughputTest {

    private static final int ID_COUNT = 50000;
    private static final long REQUIRED_IDS_PER_SECOND = 10000;

    @Test
    void generatesAtLeastTenThousandIdsPerSecond() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(
                SnowflakeConfig.defaults(), new WorkerId(0, 0), new SystemTimeSource());

        long startNanos = System.nanoTime();
        for (int i = 0; i < ID_COUNT; i++) {
            generator.nextId();
        }
        long elapsedNanos = System.nanoTime() - startNanos;

        double idsPerSecond = ID_COUNT / (elapsedNanos / 1000000000.0);
        assertThat(idsPerSecond).isGreaterThanOrEqualTo(REQUIRED_IDS_PER_SECOND);
    }
}
