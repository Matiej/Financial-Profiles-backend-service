package com.emat.reapi.ncalculator.infra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(value = NumerologyDateCalculatorDocument.COLLECTION_NAME)
@TypeAlias(value = NumerologyDateCalculatorDocument.COLLECTION_NAME)
public class NumerologyDateCalculatorDocument {

    public static final String COLLECTION_NAME = "numerology_date_calculators";

    @Id
    private String id;
    private String requestedBrithDate;
    private String requestedReferenceDate;
    private String mainVibration;
    private int personalYear;
    private int yearOfGlobalEnergy;
    private int numerologyYear;
    private int personalMonth;
    private int worldDayVibration;
    private int personalDay;
    private String userName;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @Version
    private Long version;

    public NumerologyDateCalculatorDocument(String requestedBrithDate, String requestedReferenceDate, String mainVibration, int personalYear, int yearOfGlobalEnergy, int numerologyYear, int personalMonth, int worldDayVibration, int personalDay, String userName) {
        this.requestedBrithDate = requestedBrithDate;
        this.requestedReferenceDate = requestedReferenceDate;
        this.mainVibration = mainVibration;
        this.personalYear = personalYear;
        this.yearOfGlobalEnergy = yearOfGlobalEnergy;
        this.numerologyYear = numerologyYear;
        this.personalMonth = personalMonth;
        this.worldDayVibration = worldDayVibration;
        this.personalDay = personalDay;
        this.userName = userName;
    }
}

