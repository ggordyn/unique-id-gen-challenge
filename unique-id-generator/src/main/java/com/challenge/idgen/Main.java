package com.challenge.idgen;

import com.challenge.idgen.assign.EnvironmentWorkerIdAssigner;
import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.exception.InvalidConfigurationException;
import com.challenge.idgen.model.DecodedId;
import com.challenge.idgen.snowflake.SnowflakeIdCodec;
import com.challenge.idgen.snowflake.SnowflakeIdGenerator;
import com.challenge.idgen.time.SystemTimeSource;

import java.util.Random;

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

    // Falls back to a random worker ID if the environment variables are not set or invalid, so
    // this demo still runs with zero configuration (not stable across restarts and isntances)
    private static WorkerId resolveWorkerId(SnowflakeConfig config) {
        try {
            return new EnvironmentWorkerIdAssigner().assign(config);
        } catch (InvalidConfigurationException e) {
            return randomWorkerId(config);
        }
    }

    static WorkerId randomWorkerId(SnowflakeConfig config) {
        Random random = new Random();
        return new WorkerId(random.nextInt(config.maxDatacenterId() + 1), random.nextInt(config.maxWorkerId() + 1));
    }
}
