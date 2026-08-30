package com.challenge.idgen;

import com.challenge.idgen.assign.EnvironmentWorkerIdAssigner;
import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.model.DecodedId;
import com.challenge.idgen.snowflake.SnowflakeIdCodec;
import com.challenge.idgen.snowflake.SnowflakeIdGenerator;
import com.challenge.idgen.time.SystemTimeSource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorIntegrationTest {

    @Test
    void generatesAndDecodesAnIdEndToEnd() {
        SnowflakeConfig config = SnowflakeConfig.defaults();
        WorkerId workerId = new EnvironmentWorkerIdAssigner(
                Map.of("SNOWFLAKE_DATACENTER_ID", "5", "SNOWFLAKE_WORKER_ID", "9"))
                .assign(config);
        IdGenerator generator = new SnowflakeIdGenerator(config, workerId, new SystemTimeSource());

        long id = generator.nextId();
        DecodedId decoded = SnowflakeIdCodec.decode(id, config);

        assertThat(decoded.id()).isEqualTo(id);
        assertThat(decoded.datacenterId()).isEqualTo(5);
        assertThat(decoded.workerId()).isEqualTo(9);
        assertThat(decoded.sequence()).isEqualTo(0);
        assertThat(decoded.timestamp()).isGreaterThanOrEqualTo(config.epochMillis());
    }
}
