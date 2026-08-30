package com.challenge.idgen.assign;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;

public interface WorkerIdAssigner {

    WorkerId assign(SnowflakeConfig config);
}
