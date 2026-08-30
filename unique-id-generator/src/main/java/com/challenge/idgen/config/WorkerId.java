package com.challenge.idgen.config;

import com.challenge.idgen.exception.InvalidConfigurationException;

public record WorkerId(int datacenterId, int workerId) {

    public void validate(SnowflakeConfig config) {
        if (datacenterId < 0 || datacenterId > config.maxDatacenterId()) {
            throw new InvalidConfigurationException(
                    "datacenterId must be in [0, " + config.maxDatacenterId() + "], got: " + datacenterId);
        }
        if (workerId < 0 || workerId > config.maxWorkerId()) {
            throw new InvalidConfigurationException(
                    "workerId must be in [0, " + config.maxWorkerId() + "], got: " + workerId);
        }
    }
}
