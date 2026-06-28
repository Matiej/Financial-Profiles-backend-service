package com.emat.reapi.ncalculator.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

public class MoonVirgoDatesDictionary {
    private static final List<MoonVirgo> MOON_VIRGO_DATES = List.of(
            new MoonVirgo(toInstant("2025-09-21"), "2026", 1),
            new MoonVirgo(toInstant("2026-09-10"), "2027", 2),
            new MoonVirgo(toInstant("2027-08-31"), "2028", 3),
            new MoonVirgo(toInstant("2028-09-19"), "2029", 4),
            new MoonVirgo(toInstant("2029-09-08"), "2030", 5),
            new MoonVirgo(toInstant("2030-08-28"), "2031", 6),
            new MoonVirgo(toInstant("2031-09-16"), "2032", 7),
            new MoonVirgo(toInstant("2032-09-04"), "2033", 8),
            new MoonVirgo(toInstant("2033-08-24"), "2034", 9),
            new MoonVirgo(toInstant("2034-09-13"), "2035", 1),
            new MoonVirgo(toInstant("2035-09-03"), "2036", 2)
    );

    public static List<MoonVirgo> getAll() {
        return MOON_VIRGO_DATES;
    }

    public static MoonVirgo findByCoveredYear(String year) {
        return MOON_VIRGO_DATES.stream()
                .filter(m -> m.coveredYear().equals(year))
                .findFirst()
                .orElse(null);
    }

    public static Integer findCurrentYearVibration() {
        MoonVirgo current = findCurrent();
        return current != null ? current.yearVibration() : null;
    }

    public static String findCurrentCoveredYear() {
        var current = findCurrent();
        return current != null ? current.coveredYear() : null;
    }

    public static MoonVirgo findCurrent() {
        return findFor(Instant.now());
    }

    public static MoonVirgo findFor(Instant at) {

        List<MoonVirgo> sorted = MOON_VIRGO_DATES.stream()
                .sorted(Comparator.comparing(MoonVirgo::startDate))
                .toList();

        if (sorted.isEmpty()) return null;

        MoonVirgo previous = null;
        for (MoonVirgo mv : sorted) {
            if (mv.startDate().isAfter(at)) {

                return previous;
            }
            previous = mv;
        }

        return previous;
    }

    private static Instant toInstant(String date) {
        return LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static MoonVirgo findNext() {
        Instant now = Instant.now();
        return MOON_VIRGO_DATES.stream()
                .filter(m -> m.startDate().isAfter(now))
                .min(Comparator.comparing(MoonVirgo::startDate))
                .orElse(null);
    }

}
