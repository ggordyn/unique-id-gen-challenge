package com.challenge.idgen.snowflake;

import com.challenge.idgen.config.SnowflakeConfig;
import com.challenge.idgen.config.WorkerId;
import com.challenge.idgen.model.DecodedId;
import com.challenge.idgen.time.SystemTimeSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// We use the real SystemTimeSource to prove thread-safety against a real clock.
class SnowflakeIdGeneratorConcurrencyTest {

    private static final int THREAD_COUNT = 50_000;

    @Test
    void concurrentCallsProduceNoDuplicateIds() throws InterruptedException, ExecutionException {
        SnowflakeConfig config = SnowflakeConfig.defaults();
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config, new WorkerId(1, 1), new SystemTimeSource());

        List<Callable<Long>> tasks = IntStream.range(0, THREAD_COUNT)
                .<Callable<Long>>mapToObj(i -> generator::nextId)
                .collect(Collectors.toList());

        List<Future<Long>> futures;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            futures = executor.invokeAll(tasks);
        }
        long testEnd = System.currentTimeMillis();

        List<Long> ids = new ArrayList<>(futures.size());
        for (Future<Long> future : futures) {
            ids.add(future.get());
        }

        assertThat(ids).hasSize(THREAD_COUNT);
        assertThat(ids).doesNotHaveDuplicates();

        // We check that the result falls withihn a sane timestamp range, but we don't assert the order 
        // of timestamps because we can't guarantee that the virtual threads will execute in order.
        for (long id : ids) {
            DecodedId decoded = SnowflakeIdCodec.decode(id, config);
            assertThat(decoded.timestamp()).isBetween(config.epochMillis(), testEnd);
        }
    }
}
