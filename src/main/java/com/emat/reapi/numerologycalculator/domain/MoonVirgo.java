package com.emat.reapi.numerologycalculator.domain;

import java.time.Instant;

public record MoonVirgo(
        Instant startDate,
        String coveredYear,
        Integer yearVibration
) {
}
