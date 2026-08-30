package com.challenge.idgen;

import com.challenge.idgen.assign.EnvironmentWorkerIdAssigner;
import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.exception.InvalidConfigurationException;
import com.challenge.idgen.model.DecodedId;
import com.challenge.idgen.snowflake.SnowflakeIdCodec;
import com.challenge.idgen.snowflake.SnowflakeIdGenerator;
import com.challenge.idgen.time.SystemTimeSource;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SnowflakeConfig config = SnowflakeConfig.defaults();
        WorkerId workerId = resolveWorkerId(config);
        IdGenerator generator = new SnowflakeIdGenerator(config, workerId, new SystemTimeSource());

        for (int i = 0; i < 5; i++) {
            long id = generator.nextId();
            DecodedId decoded = SnowflakeIdCodec.decode(id, config);
            System.out.printf("id=%d %s%n", id, decoded);
        }
    }

    // Fallback to datacenter/worker 0,0 if the environment variables are not set or invalid
    private static WorkerId resolveWorkerId(SnowflakeConfig config) {
        try {
            return new EnvironmentWorkerIdAssigner().assign(config);
        } catch (InvalidConfigurationException e) {
            return new WorkerId(0, 0);
        }
    }
}
