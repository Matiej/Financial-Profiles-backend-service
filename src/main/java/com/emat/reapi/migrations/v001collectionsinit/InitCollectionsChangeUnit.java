package com.emat.reapi.migrations.v001collectionsinit;

import com.emat.reapi.clienttalytest.infra.ClientAnswerDocument;
import com.emat.reapi.migrations.CollectionInitializer;
import com.emat.reapi.ncalculator.infra.NumerologyDateCalculatorDocument;
import com.emat.reapi.ncalculator.infra.NumerologyPhaseCalculatorDocument;
import com.emat.reapi.profileanalysis.infra.InsightReportDocument;
import com.emat.reapi.profileanalysis.infra.ReportJobDocument;
import com.emat.reapi.statement.infra.StatementDefinitionDocument;
import io.mongock.api.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Slf4j
@ChangeUnit(
        id = "v001_collections.init",
        order = "001",
        author = "profiler-service"
)
@AllArgsConstructor
public class InitCollectionsChangeUnit {
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    @BeforeExecution
    public void beforeExecution() {
        initReportJobCollection();
        initClientAnswerCollection();
        initNumerologyDateCalculatorCollection();
        initNumerologyPhaseCalculatorCollection();
        initInsightReportCollection();
        initStatementDefinitionCollection();
    }

    @Execution
    public void execution() {
        //  no-ops
    }

    @RollbackExecution
    public void rollback() {
        // no-ops
    }

    @RollbackBeforeExecution
    public void rollbackBeforeExecution() {
        //no-ops
    }

    private void initReportJobCollection() {
        CollectionInitializer.initializeCollection(
                mongoTemplate,
                mongoMappingContext,
                ReportJobDocument.COLLECTION_NAME,
                ReportJobDocument.class
        );
        log.info("Initialized collection: {}", ReportJobDocument.COLLECTION_NAME);
    }

    private void initClientAnswerCollection() {
        CollectionInitializer.initializeCollection(
                mongoTemplate,
                mongoMappingContext,
                ClientAnswerDocument.COLLECTION_NAME,
                ClientAnswerDocument.class
        );
        log.info("Initialized collection: {}", ClientAnswerDocument.COLLECTION_NAME);
    }

    private void initNumerologyDateCalculatorCollection() {
        CollectionInitializer.initializeCollection(
                mongoTemplate,
                mongoMappingContext,
                NumerologyDateCalculatorDocument.COLLECTION_NAME,
                NumerologyDateCalculatorDocument.class
        );
        log.info("Initialized collection: {}", NumerologyDateCalculatorDocument.COLLECTION_NAME);
    }

    private void initNumerologyPhaseCalculatorCollection() {
        CollectionInitializer.initializeCollection(
                mongoTemplate,
                mongoMappingContext,
                NumerologyPhaseCalculatorDocument.COLLECTION_NAME,
                NumerologyPhaseCalculatorDocument.class
        );
        log.info("Initialized collection: {}", NumerologyPhaseCalculatorDocument.COLLECTION_NAME);
    }

    private void initInsightReportCollection() {
        CollectionInitializer.initializeCollection(
                mongoTemplate,
                mongoMappingContext,
                InsightReportDocument.COLLECTION_NAME,
                InsightReportDocument.class
        );
        log.info("Initialized collection: {}", InsightReportDocument.COLLECTION_NAME);
    }

    private void initStatementDefinitionCollection() {
        CollectionInitializer.initializeCollection(
                mongoTemplate,
                mongoMappingContext,
                StatementDefinitionDocument.COLLECTION_NAME,
                StatementDefinitionDocument.class
        );
        log.info("Initialized collection: {}", StatementDefinitionDocument.COLLECTION_NAME);
    }

}
