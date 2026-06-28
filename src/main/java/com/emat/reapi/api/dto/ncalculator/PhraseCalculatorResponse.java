package com.emat.reapi.api.dto.ncalculator;

import com.emat.reapi.numerologycalculator.domain.NumerologyPhraseCalculator;

public record PhraseCalculatorResponse(
        String vowelsResult,
        String consonantsResult,
        String vibration
) {

    public static PhraseCalculatorResponse fromDomain(NumerologyPhraseCalculator numerologyPhraseCalculator) {
        return new PhraseCalculatorResponse(
                numerologyPhraseCalculator.getVowelsResult(),
                numerologyPhraseCalculator.getConsonantsResult(),
                numerologyPhraseCalculator.getVibration()
        );
    }
}
