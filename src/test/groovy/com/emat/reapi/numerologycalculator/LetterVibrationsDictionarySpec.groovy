package com.emat.reapi.numerologycalculator

import com.emat.reapi.numerologycalculator.domain.LetterVibrationsDictionary
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pure-data unit tests for {@code LetterVibrationsDictionary}.
 *
 * NumerologyCalculatorControllerSpec checks a few concrete phrases; this spec instead
 * pins the dictionary's structural invariants (full alphabet coverage, value range,
 * vowel set) which the controller tests do not assert.
 */
class LetterVibrationsDictionarySpec extends Specification {

    def "every letter maps to a value between 1 and 9"() {
        expect:
        LetterVibrationsDictionary.LETTER_VIBRATION.values().every { it >= 1 && it <= 9 }
    }

    def "all vowels are present in the vibration map"() {
        expect:
        LetterVibrationsDictionary.VOWELS.every { LetterVibrationsDictionary.LETTER_VIBRATION.containsKey(it) }
    }

    def "VOWELS set contains exactly the expected Polish vowels"() {
        expect:
        LetterVibrationsDictionary.VOWELS == (['A', 'Ą', 'E', 'Ę', 'I', 'O', 'Ó', 'U', 'Y'].collect { it as char } as Set)
    }

    def "Polish diacritics are all covered"() {
        given:
        def diacritics = ['Ą', 'Ę', 'Ś', 'Ć', 'Ź', 'Ż', 'Ł', 'Ń', 'Ó'].collect { it as char }

        expect:
        diacritics.every { LetterVibrationsDictionary.LETTER_VIBRATION.containsKey(it) }
    }

    @Unroll
    def "letter #letter maps to #value"() {
        expect:
        LetterVibrationsDictionary.LETTER_VIBRATION[(letter as char)] == value

        where:
        letter | value
        'A'    | 1
        'Ś'    | 1
        'J'    | 1
        'B'    | 2
        'Ć'    | 3
        'U'    | 3
        'D'    | 4
        'E'    | 5
        'O'    | 6
        'G'    | 7
        'H'    | 8
        'Ź'    | 8
        'I'    | 9
        'R'    | 9
    }
}
