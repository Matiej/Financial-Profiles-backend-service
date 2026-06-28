package com.emat.reapi.migrations.v008profilesinit;

import com.emat.reapi.statement.domain.Profile;
import com.emat.reapi.statement.infra.ProfileDocument;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@Slf4j
@ChangeUnit(
        id = "v008_profiles.add",
        order = "008",
        author = "profiler-service"
)
@AllArgsConstructor
public class AddProfilesChangeUnit {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void execution() {
        long count = mongoTemplate.getCollection(ProfileDocument.COLLECTION_NAME).countDocuments();
        if (count > 0) {
            log.info("Collection {} already contains {} documents. Skipping seed.",
                    ProfileDocument.COLLECTION_NAME, count);
            return;
        }

        List<ProfileDocument> documents = ProfilesSeed.ALL.stream()
                .map(ProfileDocument::toDocument)
                .toList();

        mongoTemplate.insert(documents, ProfileDocument.COLLECTION_NAME);

        log.info("Inserted {} profiles into collection {}",
                documents.size(), ProfileDocument.COLLECTION_NAME);
    }

    @RollbackExecution
    public void rollback() {
        List<String> ids = ProfilesSeed.ALL.stream()
                .map(Profile::getId)
                .toList();

        Query query = Query.query(Criteria.where("_id").in(ids));

        var result = mongoTemplate.remove(query, ProfileDocument.COLLECTION_NAME);

        log.info("Rollback: removed {} profiles from collection {}",
                result.getDeletedCount(), ProfileDocument.COLLECTION_NAME);
    }
}
