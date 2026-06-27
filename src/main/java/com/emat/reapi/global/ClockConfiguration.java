package com.emat.reapi.global;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfiguration {

    /**
     * System UTC clock used for all "now" lookups. Injected so that time-dependent
     * logic (e.g. the global-energy year in numerology) can be pinned in tests.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
