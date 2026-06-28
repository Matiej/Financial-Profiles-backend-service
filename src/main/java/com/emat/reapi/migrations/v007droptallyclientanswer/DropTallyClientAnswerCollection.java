package com.emat.reapi.migrations.v007droptallyclientanswer;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Drops the legacy Tally collection {@code client_answer}, removed together with the
 * Tally flow. The collection held only Tally-ingested client answers (write side was the
 * Tally webhook, now deleted); it is empty in all environments.
 *
 * <p>The historical {@code v001} change unit that originally created this collection is
 * intentionally left in place (already executed on prod, tracked by id) — its source no
 * longer references the removed {@code ClientAnswerDocument}, which is safe because Mongock
 * skips already-executed change units by id and does not checksum their source.
 */
@Slf4j
@AllArgsConstructor
@ChangeUnit(
        id = "v007_drop-tally-client-answer.collection",
        order = "007",
        author = "profiler-service"
)
public class DropTallyClientAnswerCollection {

    private static final String COLLECTION = "client_answer";

    private final MongoTemplate mongoTemplate;

    @Execution
    public void execution() {
        if (mongoTemplate.collectionExists(COLLECTION)) {
            mongoTemplate.dropCollection(COLLECTION);
            log.info("Dropped legacy Tally collection: {}", COLLECTION);
        } else {
            log.info("Legacy Tally collection '{}' not present, nothing to drop", COLLECTION);
        }
    }

    @RollbackExecution
    public void rollback() {
        // No-op: the dropped collection only held legacy Tally data; we do not recreate it.
    }
}
