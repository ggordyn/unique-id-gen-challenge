package com.challenge.idgen.time;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemTimeSourceTest {

    @Test
    void currentTimeMillisMatchesWallClock() {
        TimeSource timeSource = new SystemTimeSource();

        long before = System.currentTimeMillis();
        long actual = timeSource.currentTimeMillis();
        long after = System.currentTimeMillis();

        assertThat(actual).isBetween(before, after);
    }
}
