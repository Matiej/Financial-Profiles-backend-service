package com.emat.reapi.statement.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class StatementDefinition {
    private String statementId;
    private StatementProfile category;
    private String statementKey;
    private List<StatementTypeDefinition> statementTypeDefinitions;
}
