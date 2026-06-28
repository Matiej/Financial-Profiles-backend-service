package com.emat.reapi.ncalculator.domain

import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Pure-logic unit tests for {@code MoonVirgoDatesDictionary} (converted from JUnit).
 *
 * {@code findFor}/{@code findByCoveredYear}/{@code getAll} are pinned deterministically;
 * {@code findCurrent}/{@code findNext} depend on {@code Instant.now()} (no Clock seam),
 * so they are asserted as time-independent invariants.
 */
class MoonVirgoDatesDictionarySpec extends Specification {

    private static Instant at(String isoDate) {
        LocalDate.parse(isoDate).atStartOfDay().toInstant(ZoneOffset.UTC)
    }

    def "getAll returns all 11 entries"() {
        expect:
        MoonVirgoDatesDictionary.getAll().size() == 11
    }

    def "findByCoveredYear returns the matching entry or null"() {
        expect:
        MoonVirgoDatesDictionary.findByCoveredYear("2026").yearVibration() == 1
        MoonVirgoDatesDictionary.findByCoveredYear("2026").startDate() == at("2025-09-21")
        MoonVirgoDatesDictionary.findByCoveredYear("2030").yearVibration() == 5
        MoonVirgoDatesDictionary.findByCoveredYear("9999") == null
    }

    def "findFor resolves the entry whose start date is the latest not after the instant"() {
        expect: "before the first entry -> null"
        MoonVirgoDatesDictionary.findFor(at("2025-01-01")) == null

        and: "inside the first window -> the 2026 entry"
        MoonVirgoDatesDictionary.findFor(at("2025-12-01")).coveredYear() == "2026"

        and: "exactly on a start date -> that entry (start is inclusive)"
        MoonVirgoDatesDictionary.findFor(at("2026-09-10")).coveredYear() == "2027"

        and: "after the last entry -> the last entry"
        def last = MoonVirgoDatesDictionary.findFor(at("2040-01-01"))
        last.coveredYear() == "2036"
        last.yearVibration() == 2
    }

    def "findCurrent equals findFor(now) and has already started when present"() {
        given:
        def now = Instant.now()
        def current = MoonVirgoDatesDictionary.findCurrent()

        expect:
        current == MoonVirgoDatesDictionary.findFor(now)
        current == null || !current.startDate().isAfter(now)
        current == null || MoonVirgoDatesDictionary.findCurrentYearVibration() == current.yearVibration()
        current == null || MoonVirgoDatesDictionary.findCurrentCoveredYear() == current.coveredYear()
    }

    def "findNext returns the earliest future entry when present"() {
        given:
        def now = Instant.now()
        def next = MoonVirgoDatesDictionary.findNext()

        expect:
        next == null || next.startDate().isAfter(now)
        next == null || MoonVirgoDatesDictionary.getAll()
                .findAll { it.startDate().isAfter(now) }
                .every { !it.startDate().isBefore(next.startDate()) }
    }
}
