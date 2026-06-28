package com.emat.reapi.ncalculator.infra;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface NumerologyDateCalculatorRepository extends ReactiveMongoRepository<NumerologyDateCalculatorDocument, String> {
}
