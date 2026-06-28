package com.emat.reapi.api.dto.ncalculator;

import com.emat.reapi.numerologycalculator.domain.NumerologyDatesCalculator;

public record DatesCalculatorResponse(
        String mainVibration,
        int personalYear,
        int yearOfGlobalEnergy,
        int numerologyYear,
        int personalMonth,
        int worldDayVibration,
        int personalDay
        ) {

    public static DatesCalculatorResponse fromDomain(NumerologyDatesCalculator numerologyDatesCalculator) {
        return new DatesCalculatorResponse(
                numerologyDatesCalculator.getMainVibration(),
                numerologyDatesCalculator.getPersonalYear(),
                numerologyDatesCalculator.getYearOfGlobalEnergy(),
                numerologyDatesCalculator.getNumerologyYear(),
                numerologyDatesCalculator.getPersonalMonth(),
                numerologyDatesCalculator.getWorldDayVibration(),
                numerologyDatesCalculator.getPersonalDay()
        );
    }
}
