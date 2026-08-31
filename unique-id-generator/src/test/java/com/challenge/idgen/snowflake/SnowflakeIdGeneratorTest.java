package com.challenge.idgen.snowflake;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.exception.ClockMovedBackwardsException;
import com.challenge.idgen.exception.InvalidConfigurationException;
import com.challenge.idgen.model.DecodedId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeIdGeneratorTest {

    private final SnowflakeConfig config = SnowflakeConfig.defaults();
    private final WorkerId workerId = new WorkerId(3, 7);

    @Test
    void rejectsWorkerIdOutOfRangeForConfig() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis());

        assertThatThrownBy(() -> new SnowflakeIdGenerator(config, new WorkerId(99, 0), timeSource))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void idsAreMonotonicAcrossMilliseconds() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis() + 1000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, workerId, timeSource);

        long first = generator.nextId();
        timeSource.advanceTo(config.epochMillis() + 1001);
        long second = generator.nextId();

        assertThat(second).isGreaterThan(first);
    }

    @Test
    void sequenceIncrementsWithinSameMillisecond() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis() + 1000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, workerId, timeSource);

        DecodedId first = SnowflakeIdCodec.decode(generator.nextId(), config);
        DecodedId second = SnowflakeIdCodec.decode(generator.nextId(), config);

        assertThat(second.timestamp()).isEqualTo(first.timestamp());
        assertThat(second.sequence()).isEqualTo(first.sequence() + 1);
    }

    @Test
    void sequenceResetsOnNewMillisecond() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis() + 1000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, workerId, timeSource);

        generator.nextId();
        generator.nextId();
        timeSource.advanceTo(config.epochMillis() + 1001);

        DecodedId decoded = SnowflakeIdCodec.decode(generator.nextId(), config);

        assertThat(decoded.sequence()).isEqualTo(0);
    }

    @Test
    void sequenceOverflowForcesWaitForNextMillisecond() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis() + 1000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, workerId, timeSource);

        long lastId = -1;
        for (int i = 0; i <= config.maxSequence(); i++) {
            lastId = generator.nextId();
        }
        DecodedId beforeOverflow = SnowflakeIdCodec.decode(lastId, config);
        assertThat(beforeOverflow.sequence()).isEqualTo(config.maxSequence());

        DecodedId afterOverflow = SnowflakeIdCodec.decode(generator.nextId(), config);

        assertThat(afterOverflow.timestamp()).isEqualTo(beforeOverflow.timestamp() + 1);
        assertThat(afterOverflow.sequence()).isEqualTo(0);
    }

    @Test
    void smallBackwardClockDriftReusesTimestampAndExtendsSequence() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis() + 1000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, workerId, timeSource);

        generator.nextId();
        timeSource.advanceTo(config.epochMillis() + 1000 - config.maxBackwardDriftMillis());

        DecodedId decoded = SnowflakeIdCodec.decode(generator.nextId(), config);

        assertThat(decoded.timestamp()).isEqualTo(config.epochMillis() + 1000);
        assertThat(decoded.sequence()).isEqualTo(1);
    }

    @Test
    void smallBackwardClockDriftFallsBackToWaitOnceSequenceOverflows() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis() + 1000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, workerId, timeSource);

        for (int i = 0; i <= config.maxSequence(); i++) {
            generator.nextId();
        }
        timeSource.advanceTo(config.epochMillis() + 1000 - config.maxBackwardDriftMillis());

        DecodedId decoded = SnowflakeIdCodec.decode(generator.nextId(), config);

        assertThat(decoded.timestamp()).isEqualTo(config.epochMillis() + 1001);
        assertThat(decoded.sequence()).isEqualTo(0);
    }

    @Test
    void largeBackwardClockDriftThrowsWithDriftAmount() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis() + 1000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, workerId, timeSource);
        long driftBack = config.maxBackwardDriftMillis() + 1;

        generator.nextId();
        timeSource.advanceTo(config.epochMillis() + 1000 - driftBack);

        assertThatThrownBy(generator::nextId)
                .isInstanceOf(ClockMovedBackwardsException.class)
                .extracting(e -> ((ClockMovedBackwardsException) e).driftMillis())
                .isEqualTo(driftBack);
    }

    @Test
    void decodedOutputMatchesConfiguredWorkerId() {
        FakeTimeSource timeSource = new FakeTimeSource(config.epochMillis() + 1000);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, workerId, timeSource);

        DecodedId decoded = SnowflakeIdCodec.decode(generator.nextId(), config);

        assertThat(decoded.datacenterId()).isEqualTo(3);
        assertThat(decoded.workerId()).isEqualTo(7);
    }
}
