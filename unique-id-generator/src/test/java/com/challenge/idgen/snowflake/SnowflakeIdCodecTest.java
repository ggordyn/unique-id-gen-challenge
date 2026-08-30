package com.challenge.idgen.snowflake;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.model.DecodedId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnowflakeIdCodecTest {

    private final SnowflakeConfig config = SnowflakeConfig.defaults();

    @Test
    void roundTripsRepresentativeValues() {
        long timestamp = config.epochMillis() + 123456789L;

        long id = SnowflakeIdCodec.encode(timestamp, 3, 7, 42, config);
        DecodedId decoded = SnowflakeIdCodec.decode(id, config);

        assertThat(decoded.id()).isEqualTo(id);
        assertThat(decoded.timestamp()).isEqualTo(timestamp);
        assertThat(decoded.datacenterId()).isEqualTo(3);
        assertThat(decoded.workerId()).isEqualTo(7);
        assertThat(decoded.sequence()).isEqualTo(42);
    }

    @Test
    void roundTripsMaxBoundaryValues() {
        long timestamp = config.epochMillis();

        long id = SnowflakeIdCodec.encode(timestamp, 31, 31, 4095, config);
        DecodedId decoded = SnowflakeIdCodec.decode(id, config);

        assertThat(decoded.timestamp()).isEqualTo(timestamp);
        assertThat(decoded.datacenterId()).isEqualTo(31);
        assertThat(decoded.workerId()).isEqualTo(31);
        assertThat(decoded.sequence()).isEqualTo(4095);
    }

    @Test
    void roundTripsMinBoundaryValues() {
        long timestamp = config.epochMillis();

        long id = SnowflakeIdCodec.encode(timestamp, 0, 0, 0, config);
        DecodedId decoded = SnowflakeIdCodec.decode(id, config);

        assertThat(decoded.timestamp()).isEqualTo(timestamp);
        assertThat(decoded.datacenterId()).isEqualTo(0);
        assertThat(decoded.workerId()).isEqualTo(0);
        assertThat(decoded.sequence()).isEqualTo(0);
    }

    // Default layout shifts: sequence occupies bits [0,12), worker [12,17), datacenter
    // [17,22), timestamp [22,63). Hand-computed for elapsed=1, datacenterId=1, workerId=1,
    // sequence=1: (1<<22) + (1<<17) + (1<<12) + 1 = 4329473.
    @Test
    void encodesToIndependentlyHandComputedBits() {
        long timestamp = config.epochMillis() + 1;

        long id = SnowflakeIdCodec.encode(timestamp, 1, 1, 1, config);

        assertThat(id).isEqualTo(4329473L);
    }

    @Test
    void decodesIndependentlyHandComputedBits() {
        DecodedId decoded = SnowflakeIdCodec.decode(4329473L, config);

        assertThat(decoded.timestamp()).isEqualTo(config.epochMillis() + 1);
        assertThat(decoded.datacenterId()).isEqualTo(1);
        assertThat(decoded.workerId()).isEqualTo(1);
        assertThat(decoded.sequence()).isEqualTo(1);
    }

    @Test
    void roundTripsUnderNonDefaultBitSplit() {
        SnowflakeConfig customConfig = new SnowflakeConfig(0, 3, 3, 16, 5);
        long timestamp = 100;

        long id = SnowflakeIdCodec.encode(timestamp, 7, 7, 65535, customConfig);
        DecodedId decoded = SnowflakeIdCodec.decode(id, customConfig);

        assertThat(decoded.timestamp()).isEqualTo(timestamp);
        assertThat(decoded.datacenterId()).isEqualTo(7);
        assertThat(decoded.workerId()).isEqualTo(7);
        assertThat(decoded.sequence()).isEqualTo(65535);
    }

    @Test
    void signBitIsAlwaysZeroForInRangeInputs() {
        long maxElapsed = (1L << 41) - 1;
        long timestamp = config.epochMillis() + maxElapsed;

        long id = SnowflakeIdCodec.encode(timestamp, 31, 31, 4095, config);

        assertThat(id).isNotNegative();
    }
}
