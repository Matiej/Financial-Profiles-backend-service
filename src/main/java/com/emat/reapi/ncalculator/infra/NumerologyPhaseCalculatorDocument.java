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
@Document(value = NumerologyPhaseCalculatorDocument.COLLECTION_NAME)
@TypeAlias(value = NumerologyPhaseCalculatorDocument.COLLECTION_NAME)
public class NumerologyPhaseCalculatorDocument {

    public final static String COLLECTION_NAME = "numerology_phase_calculators";

    @Id
    private String id;
    private String requestedPhase;
    private String vowelsResult;
    private String consonantsResult;
    private String vibration;
    private String userName;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @Version
    private Long version;
}
