package com.emat.reapi.numerologycalculator.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NumerologyPhraseCalculator {
    private String vowelsResult;
    private String consonantsResult;
    private String vibration;
}
