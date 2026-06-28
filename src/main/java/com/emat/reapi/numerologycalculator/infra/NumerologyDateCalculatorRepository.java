package com.emat.reapi.numerologycalculator.infra;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface NumerologyDateCalculatorRepository extends ReactiveMongoRepository<NumerologyDateCalculatorDocument, String> {
}
