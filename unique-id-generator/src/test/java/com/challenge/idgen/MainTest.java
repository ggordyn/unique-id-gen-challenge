package com.challenge.idgen;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    private final SnowflakeConfig config = SnowflakeConfig.defaults();

    @Test
    void randomWorkerIdIsAlwaysInRange() {
        for (int i = 0; i < 1000; i++) {
            WorkerId workerId = Main.randomWorkerId(config);

            assertThat(workerId.datacenterId()).isBetween(0, config.maxDatacenterId());
            assertThat(workerId.workerId()).isBetween(0, config.maxWorkerId());
        }
    }
}
