package com.challenge.idgen.config;

import com.challenge.idgen.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeConfigTest {

    @Test
    void defaultsUseClassicFiveFiveTwelveSplit() {
        SnowflakeConfig config = SnowflakeConfig.defaults();

        assertThat(config.datacenterIdBits()).isEqualTo(5);
        assertThat(config.workerIdBits()).isEqualTo(5);
        assertThat(config.sequenceBits()).isEqualTo(12);
        assertThat(config.maxDatacenterId()).isEqualTo(31);
        assertThat(config.maxWorkerId()).isEqualTo(31);
        assertThat(config.maxSequence()).isEqualTo(4095);
        assertThat(config.maxBackwardDriftMillis()).isEqualTo(10);
    }

    @Test
    void rejectsBitWidthsNotSummingToTwentyTwo() {
        assertThatThrownBy(() -> new SnowflakeConfig(0, 5, 5, 11, 10))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("must equal 22");
    }

    @Test
    void rejectsZeroBitWidth() {
        assertThatThrownBy(() -> new SnowflakeConfig(0, 0, 10, 12, 10))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("must each be > 0");
    }

    @Test
    void rejectsNegativeBitWidth() {
        assertThatThrownBy(() -> new SnowflakeConfig(0, -1, 11, 12, 10))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("must each be > 0");
    }

    @Test
    void rejectsNegativeDriftTolerance() {
        assertThatThrownBy(() -> new SnowflakeConfig(0, 5, 5, 12, -1))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("maxBackwardDriftMillis");
    }

    @Test
    void acceptsNonDefaultBitSplit() {
        SnowflakeConfig config = new SnowflakeConfig(0, 3, 3, 16, 5);

        assertThat(config.maxDatacenterId()).isEqualTo(7);
        assertThat(config.maxWorkerId()).isEqualTo(7);
        assertThat(config.maxSequence()).isEqualTo(65535);
        assertThat(config.workerIdShift()).isEqualTo(16);
        assertThat(config.datacenterIdShift()).isEqualTo(19);
        assertThat(config.timestampShift()).isEqualTo(22);
    }
}
