package com.emat.reapi.migrations.v002statementdefinitioninit;

import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.infra.StatementDefinitionDocument;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ChangeUnit(
        id = "v002_statement-definitions.add",
        order = "002",
        author = "profiler-service"
)
@AllArgsConstructor
public class AddStatementDefinitionsChangeUnit {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void execution() {
        long count = mongoTemplate.getCollection(StatementDefinitionDocument.COLLECTION_NAME).countDocuments();
        if (count > 0) {
            log.info("Collection {} already contains {} documents. Skipping seed.",
                    StatementDefinitionDocument.COLLECTION_NAME, count);
            return;
        }

        List<StatementDefinition> allDefinitions = StatementDefinitionsSeed.ALL;

        Instant base = Instant.now();
        List<StatementDefinitionDocument> documents = new ArrayList<>(allDefinitions.size());
        for (int i = 0; i < allDefinitions.size(); i++) {
            StatementDefinitionDocument document = StatementDefinitionDocument.toDocument(allDefinitions.get(i));
            Instant timestamp = base.plusMillis(i);
            document.setCreatedAt(timestamp);
            document.setUpdatedAt(timestamp);
            documents.add(document);
        }

        mongoTemplate.insert(documents, StatementDefinitionDocument.COLLECTION_NAME);

        log.info("Inserted {} statement definitions into collection {}",
                documents.size(), StatementDefinitionDocument.COLLECTION_NAME);
    }

    @RollbackExecution
    public void rollback() {
        List<String> statementKeys = StatementDefinitionsSeed.ALL.stream()
                .map(StatementDefinition::getStatementKey)
                .toList();

        Query query = Query.query(
                Criteria.where("statementKey").in(statementKeys)
        );

        var result = mongoTemplate.remove(
                query,
                StatementDefinitionDocument.COLLECTION_NAME
        );

        log.info("Rollback: removed {} statement definitions from collection {}",
                result.getDeletedCount(), StatementDefinitionDocument.COLLECTION_NAME);
    }
}
