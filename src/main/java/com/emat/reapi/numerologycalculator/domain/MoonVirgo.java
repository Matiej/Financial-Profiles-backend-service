package com.emat.reapi.ncalculator.domain;

import java.time.Instant;

public record MoonVirgo(
        Instant startDate,
        String coveredYear,
        Integer yearVibration
) {
}
