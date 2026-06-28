package com.emat.reapi.ncalculator.port;

import com.emat.reapi.ncalculator.domain.*;
import com.emat.reapi.ncalculator.infra.NumerologyDateCalculatorDocument;
import com.emat.reapi.ncalculator.infra.NumerologyDateCalculatorRepository;
import com.emat.reapi.ncalculator.infra.NumerologyPhaseCalculatorDocument;
import com.emat.reapi.ncalculator.infra.NumerologyPhaseCalculatorRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@AllArgsConstructor
public class NCalculatorServiceImpl implements NCalculatorService {
    private final NumerologyPhaseCalculatorRepository numerologyPhaseCalculatorRepository;
    private final NumerologyDateCalculatorRepository numerologyDateCalculatorRepository;
    private final Clock clock;

    @Override
    public Mono<NumerologyPhraseCalculator> calculatePhrase(String phrase, String userName) {
        if (phrase == null) {
            return Mono.error(new IllegalArgumentException("Phrase cannot be null"));
        }
        var normalizedPhrase = phrase.replace(" ", "").toUpperCase();
        int vowelsSum = 0;
        int consonantsSum = 0;
        int totalSum = 0;

        for (int i = 0; i < normalizedPhrase.length(); i++) {
            char ch = normalizedPhrase.charAt(i);

            if (Character.isWhitespace(ch)) {
                continue;
            }

            if (Character.isDigit(ch)) {
                int digit = Character.digit(ch, 10);
                totalSum += digit;
                consonantsSum += digit;
                continue;
            }

            if (Character.isLetter(ch)) {
                char upper = Character.toUpperCase(ch);
                int value = LetterVibrationsDictionary.LETTER_VIBRATION.getOrDefault(upper, 0);
                totalSum += value;

                if (LetterVibrationsDictionary.VOWELS.contains(upper)) {
                    vowelsSum += value;
                } else {
                    consonantsSum += value;
                }
            }
        }
        String vibration = reduceToTwoLevelVibration(totalSum);
        String vowelsVibration = reduceToTwoLevelVibration(vowelsSum);
        String consonantsVibration = reduceToTwoLevelVibration(consonantsSum);
        NumerologyPhraseCalculator result = new NumerologyPhraseCalculator(
                vowelsVibration,
                consonantsVibration,
                vibration
        );
        NumerologyPhaseCalculatorDocument document = new NumerologyPhaseCalculatorDocument(
                null,
                phrase,
                vowelsVibration,
                consonantsVibration,
                vibration,
                userName,
                null,
                null,
                null
        );
        return numerologyPhaseCalculatorRepository
                .save(document)
                .doOnSuccess(saved ->
                        log.info("Saved numerology phrase calculation for phrase='{}', id={}",
                                normalizedPhrase, saved.getId()))
                .map(saved -> result);
    }

    private String reduceToTwoLevelVibration(int number) {
        int n = Math.abs(number);

        // already single digit – nie ma co rozbijać
        if (n < 10) {
            return String.valueOf(n);
        }

        while (true) {
            int sum = 0;
            int tmp = n;
            while (tmp > 0) {
                sum += tmp % 10;
                tmp /= 10;
            }

            // jeśli następny krok dałby już 1 cyfrę,
            // to n jest "ostatnią wielocyfrową", a sum pojedynczą cyfrą
            if (sum < 10) {
                return n + "/" + sum;
            }

            // w przeciwnym razie redukujemy dalej
            n = sum;
        }
    }

    @Override
    public Mono<NumerologyDatesCalculator> calculateDates(String birthDate, String referenceDate, String userName) {
        if (birthDate == null) {
            return Mono.error(new IllegalArgumentException("birthDate cannot be null"));
        }

        final LocalDate birth;
        final LocalDate reference;

        try {
            birth = LocalDate.parse(birthDate);
        } catch (DateTimeParseException e) {
            return Mono.error(new IllegalArgumentException("Invalid birthDate format, expected yyyy-MM-dd", e));
        }

        if (referenceDate == null || referenceDate.isBlank()) {
            reference = LocalDate.now(clock);
        } else {
            try {
                reference = LocalDate.parse(referenceDate);
            } catch (DateTimeParseException e) {
                return Mono.error(new IllegalArgumentException("Invalid referenceDate format, expected yyyy-MM-dd", e));
            }
        }

        var mainVibration = calculateMainVibration(birth);

        // 2. Rok osobisty (RO)
        int personalYear = calculatePersonalYear(birth, reference);

        // 3. Rok energii światowej (REŚ) - zawsze wg bieżącego roku (z wstrzykiwanego zegara)
        int yearOfGlobalEnergy = calculateYearOfGlobalEnergy(LocalDate.now(clock));

        // 4. Rok numerologiczny (RN)
        int numerologyYear = calculateNumerologyYear(birth, yearOfGlobalEnergy);

        // 5. Miesiąc osobisty (MO)
        int personalMonth = calculatePersonalMonth(reference, personalYear);

        // 6. Światowa Wibracja Dnia (ŚWD)
        int worldDayVibration = calculateWorldDayVibration(reference);

        // 7. Dzień osobisty
        int personalDay = calculatePersonalDay(reference, personalMonth);
        NumerologyDatesCalculator result = new NumerologyDatesCalculator(
                mainVibration,
                personalYear,
                yearOfGlobalEnergy,
                numerologyYear,
                personalMonth,
                worldDayVibration,
                personalDay
        );

        NumerologyDateCalculatorDocument document = new NumerologyDateCalculatorDocument(
                birthDate,
                reference.toString(),
                mainVibration,
                personalYear,
                yearOfGlobalEnergy,
                numerologyYear,
                personalMonth,
                worldDayVibration,
                personalDay,
                userName
        );

        return numerologyDateCalculatorRepository
                .save(document)
                .doOnSuccess(saved ->
                        log.info("Saved numerology dates calculation for birthDate='{}', referenceDate='{}', id={}",
                                birthDate, reference, saved.getId()))
                .map(saved -> result);

    }

    // 1. Wibracja Główna (WG) = suma cyfr daty urodzenia, np. 21.05.1978 -> 33/6
    private String calculateMainVibration(LocalDate birthDate) {
        int sum = sumDateDigits(birthDate);
        int reduced = reduceToSingleDigit(sum);
        return sum + "/" + reduced;
    }

    // 2. Rok osobisty (RO) – dzień + miesiąc urodzenia + "rok osobisty"
    // Rok osobisty trwa od urodzin w danym roku do kolejnych urodzin.
    private int calculatePersonalYear(LocalDate birthDate, LocalDate referenceDate) {
        int refYear = referenceDate.getYear();
        LocalDate birthdayThisYear = LocalDate.of(
                refYear,
                birthDate.getMonthValue(),
                birthDate.getDayOfMonth()
        );

        int yearForCalc = referenceDate.isBefore(birthdayThisYear)
                ? refYear - 1
                : refYear;

        int sum = sumDigits(birthDate.getDayOfMonth())
                + sumDigits(birthDate.getMonthValue())
                + sumDigits(yearForCalc);

        return reduceToSingleDigit(sum);
    }

    // 3. Rok energii światowej (REŚ) – z tabeli MoonVirgoDatesDictionary,
    // bazując na dacie br.
    private int calculateYearOfGlobalEnergy(LocalDate referenceDate) {
        Instant at = referenceDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        MoonVirgo mv = MoonVirgoDatesDictionary.findFor(at);
        return mv != null && mv.yearVibration() != null ? mv.yearVibration() : 0;
    }

    // 4. Rok numerologiczny (RN) = WG (zredukowane do jednej cyfry) + REŚ, zredukowane do jednej cyfry.
    private int calculateNumerologyYear(LocalDate birthDate, int yearOfGlobalEnergy) {
        int mainDigit = reduceToSingleDigit(sumDateDigits(birthDate));
        int sum = mainDigit + yearOfGlobalEnergy;
        return reduceToSingleDigit(sum);
    }

    // 5. Miesiąc osobisty (MO) = (suma cyfr miesiąca kalendarzowego) + Rok osobisty (RO)
    // np. 11.2025, RO=6 -> 1+1+6=8
    private int calculatePersonalMonth(LocalDate referenceDate, int personalYear) {
        int monthSum = sumDigits(referenceDate.getMonthValue());
        int sum = monthSum + personalYear;
        return reduceToSingleDigit(sum);
    }

    // 6. Światowa Wibracja Dnia (ŚWD) = suma wszystkich cyfr bieżącej daty, zredukowana do jednej cyfry
    // np. 11.11.2025 -> 1+1+1+1+2+0+2+5=13 -> 1+3=4
    private int calculateWorldDayVibration(LocalDate referenceDate) {
        int sum = sumDateDigits(referenceDate);
        return reduceToSingleDigit(sum);
    }

    // 7. Dzień osobisty = Miesiąc osobisty (MO) + suma cyfr dnia miesiąca, zredukowane do jednej cyfry
    // np. MO=8, dzień=11 -> 8+1+1=10 -> 1
    private int calculatePersonalDay(LocalDate referenceDate, int personalMonth) {
        int daySum = sumDigits(referenceDate.getDayOfMonth());
        int sum = personalMonth + daySum;
        return reduceToSingleDigit(sum);
    }

    private int reduceToSingleDigit(int number) {
        int n = Math.abs(number);
        while (n > 9) {
            int sum = 0;
            while (n > 0) {
                sum += n % 10;
                n /= 10;
            }
            n = sum;
        }
        return n;
    }

    private int sumDateDigits(LocalDate date) {
        return sumDigits(date.getDayOfMonth())
                + sumDigits(date.getMonthValue())
                + sumDigits(date.getYear());
    }

    private int sumDigits(int value) {
        int number = Math.abs(value);
        int sum = 0;
        while (number > 0) {
            sum += number % 10;
            number /= 10;
        }
        return sum;
    }
}
