package com.emat.reapi.numerologycalculator.infra;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NumerologyPhaseCalculatorRepository extends ReactiveMongoRepository<NumerologyPhaseCalculatorDocument, String> {
}
