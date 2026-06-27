package com.emat.reapi.configuration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Pins the application {@link Clock} to a fixed instant so that time-dependent
 * numerology fields (yearOfGlobalEnergy, numerologyYear) are deterministic in tests.
 */
@TestConfiguration
class TestClockConfiguration {

    static final Instant FIXED_INSTANT = Instant.parse("2025-11-11T00:00:00Z")

    @Bean
    @Primary
    Clock testClock() {
        Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
    }
}
