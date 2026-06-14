package com.emat.reapi.statement.infra;

import com.emat.reapi.statement.domain.StatementProfile;
import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementTypeDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private String statementId;
    @Indexed(name = "statementKey_idx", background = true, unique = true)
    private String statementKey;
    private StatementProfile category;
    private List<StatementTypeDefinition> statementTypeDefinitions;

    public StatementDefinitionDocument(String statementId, String statementKey, StatementProfile category, List<StatementTypeDefinition> statementTypeDefinitions) {
        this.statementId = statementId;
        this.statementKey = statementKey;
        this.category = category;
        this.statementTypeDefinitions = statementTypeDefinitions;
    }

    public StatementDefinition toDomain() {
        return new StatementDefinition(statementId, category, statementKey, statementTypeDefinitions);
    }

    public static StatementDefinitionDocument toDocument(StatementDefinition domain) {
        return new StatementDefinitionDocument(
                domain.getStatementId(),
                domain.getStatementKey(),
                domain.getCategory(),
                domain.getStatementTypeDefinitions()
        );
    }
}
