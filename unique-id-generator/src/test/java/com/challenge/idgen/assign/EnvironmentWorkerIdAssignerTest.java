package com.challenge.idgen.assign;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentWorkerIdAssignerTest {

    private final SnowflakeConfig config = SnowflakeConfig.defaults();

    @Test
    void parsesValidEnvValues() {
        EnvironmentWorkerIdAssigner assigner = new EnvironmentWorkerIdAssigner(
                Map.of("SNOWFLAKE_DATACENTER_ID", "3", "SNOWFLAKE_WORKER_ID", "7"));

        WorkerId workerId = assigner.assign(config);

        assertThat(workerId).isEqualTo(new WorkerId(3, 7));
    }

    @Test
    void throwsWhenDatacenterIdMissing() {
        EnvironmentWorkerIdAssigner assigner = new EnvironmentWorkerIdAssigner(
                Map.of("SNOWFLAKE_WORKER_ID", "7"));

        assertThatThrownBy(() -> assigner.assign(config))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("SNOWFLAKE_DATACENTER_ID");
    }

    @Test
    void throwsWhenWorkerIdMissing() {
        EnvironmentWorkerIdAssigner assigner = new EnvironmentWorkerIdAssigner(
                Map.of("SNOWFLAKE_DATACENTER_ID", "3"));

        assertThatThrownBy(() -> assigner.assign(config))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("SNOWFLAKE_WORKER_ID");
    }

    @Test
    void throwsWhenValueIsNotNumeric() {
        EnvironmentWorkerIdAssigner assigner = new EnvironmentWorkerIdAssigner(
                Map.of("SNOWFLAKE_DATACENTER_ID", "abc", "SNOWFLAKE_WORKER_ID", "7"));

        assertThatThrownBy(() -> assigner.assign(config))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("must be an integer");
    }

    @Test
    void throwsWhenValueIsOutOfRange() {
        EnvironmentWorkerIdAssigner assigner = new EnvironmentWorkerIdAssigner(
                Map.of("SNOWFLAKE_DATACENTER_ID", "32", "SNOWFLAKE_WORKER_ID", "7"));

        assertThatThrownBy(() -> assigner.assign(config))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("datacenterId");
    }
}
