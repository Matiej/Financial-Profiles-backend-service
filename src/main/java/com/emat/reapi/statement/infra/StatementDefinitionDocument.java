package com.emat.reapi.statement.infra;

import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementTypeDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(value = StatementDefinitionDocument.COLLECTION_NAME)
@TypeAlias(value = StatementDefinitionDocument.COLLECTION_NAME)
public class StatementDefinitionDocument {

    public static final String COLLECTION_NAME = "statement_definitions";

    @Id
    private String id;
    @Indexed(name = "statementKey_idx", background = true, unique = true)
    private String statementKey;
    private String profileId;
    private List<StatementTypeDefinition> statementTypeDefinitions;
    private boolean isDeleted;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public StatementDefinition toDomain() {
        return new StatementDefinition(id, profileId, statementKey, statementTypeDefinitions, isDeleted, createdAt, updatedAt);
    }

    public static StatementDefinitionDocument toDocument(StatementDefinition domain) {
        return new StatementDefinitionDocument(
                domain.getId(),
                domain.getStatementKey(),
                domain.getProfileId(),
                domain.getStatementTypeDefinitions(),
                domain.isDeleted(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
