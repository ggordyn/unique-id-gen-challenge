package com.challenge.idgen.assign;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.exception.InvalidConfigurationException;

import java.util.Map;

public class EnvironmentWorkerIdAssigner implements WorkerIdAssigner {

    private static final String DATACENTER_ID_ENV_VAR = "SNOWFLAKE_DATACENTER_ID";
    private static final String WORKER_ID_ENV_VAR = "SNOWFLAKE_WORKER_ID";

    private final Map<String, String> env;

    public EnvironmentWorkerIdAssigner() {
        this(System.getenv());
    }

    public EnvironmentWorkerIdAssigner(Map<String, String> env) {
        this.env = env;
    }

    @Override
    public WorkerId assign(SnowflakeConfig config) {
        WorkerId workerId = new WorkerId(parse(DATACENTER_ID_ENV_VAR), parse(WORKER_ID_ENV_VAR));
        workerId.validate(config);
        return workerId;
    }

    private int parse(String envVar) {
        String value = env.get(envVar);
        if (value == null) {
            throw new InvalidConfigurationException(envVar + " is not set");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException(envVar + " must be an integer, got: " + value);
        }
    }
}
