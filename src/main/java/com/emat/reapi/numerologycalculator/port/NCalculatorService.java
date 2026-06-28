package com.emat.reapi.ncalculator.port;

import com.emat.reapi.ncalculator.domain.NumerologyDatesCalculator;
import com.emat.reapi.ncalculator.domain.NumerologyPhraseCalculator;
import reactor.core.publisher.Mono;

public interface NCalculatorService {
    Mono<NumerologyPhraseCalculator> calculatePhrase(String phrase, String userName);
    Mono<NumerologyDatesCalculator> calculateDates(String birthDate, String referenceDate, String userName);
}
