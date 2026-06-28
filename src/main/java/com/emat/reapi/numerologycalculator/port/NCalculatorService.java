package com.emat.reapi.numerologycalculator.port;

import com.emat.reapi.numerologycalculator.domain.NumerologyDatesCalculator;
import com.emat.reapi.numerologycalculator.domain.NumerologyPhraseCalculator;
import reactor.core.publisher.Mono;

public interface NCalculatorService {
    Mono<NumerologyPhraseCalculator> calculatePhrase(String phrase, String userName);
    Mono<NumerologyDatesCalculator> calculateDates(String birthDate, String referenceDate, String userName);
}
