package com.challenge.idgen.config;

import com.challenge.idgen.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkerIdTest {

    private final SnowflakeConfig config = SnowflakeConfig.defaults();

    @Test
    void acceptsIdsWithinRange() {
        assertThatCode(() -> new WorkerId(0, 0).validate(config)).doesNotThrowAnyException();
        assertThatCode(() -> new WorkerId(31, 31).validate(config)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDatacenterIdAboveMax() {
        assertThatThrownBy(() -> new WorkerId(32, 0).validate(config))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("datacenterId");
    }

    @Test
    void rejectsNegativeWorkerId() {
        assertThatThrownBy(() -> new WorkerId(0, -1).validate(config))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("workerId");
    }
}
