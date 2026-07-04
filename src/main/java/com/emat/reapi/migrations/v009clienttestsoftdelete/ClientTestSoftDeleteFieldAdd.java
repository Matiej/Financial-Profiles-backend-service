package com.emat.reapi.migrations.v009clienttestsoftdelete;

import com.emat.reapi.clienttest.infra.ClientTestDocument;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Adds soft-delete support to the existing {@code client_tests} collection:
 *  - Backfills {@code deleted = false} on documents created before the field existed.
 *    Required because Mongo's {@code {deleted:false}} filter does NOT match documents where
 *    the field is absent — without this, the *AndDeletedFalse finders would hide legacy tests.
 *  - Ensures an index on {@code deleted} to support the filtered reads.
 */
@Slf4j
@AllArgsConstructor
@ChangeUnit(
        id = "v009_client-test-soft-delete-field.add",
        order = "009",
        author = "profiler-service"
)
public class ClientTestSoftDeleteFieldAdd {
    private static final String INDEX_NAME = "deleted_idx";
    private final MongoTemplate mongoTemplate;

    @Execution
    public void execution() {
        var query = new Query(Criteria.where("deleted").exists(false));
        var update = new Update().set("deleted", false);
        var result = mongoTemplate.updateMulti(query, update, ClientTestDocument.COLLECTION_NAME);
        log.info("Backfilled deleted=false on {} documents in collection {}",
                result.getModifiedCount(), ClientTestDocument.COLLECTION_NAME);

        var index = new Index()
                .on("deleted", Sort.Direction.ASC)
                .named(INDEX_NAME)
                .background();
        mongoTemplate.indexOps(ClientTestDocument.COLLECTION_NAME)
                .ensureIndex(index);
        log.info("Ensured index {} on collection {}", INDEX_NAME, ClientTestDocument.COLLECTION_NAME);
    }

    @RollbackExecution
    public void rollback() {
        // no-ops
    }
}
