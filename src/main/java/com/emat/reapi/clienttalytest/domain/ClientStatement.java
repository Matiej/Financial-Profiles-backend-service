package com.emat.reapi.clienttalytest.domain;

import com.emat.reapi.statement.domain.Statement;
import com.emat.reapi.statement.domain.StatementProfile;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ClientStatement {
    private String statementId;
    private String key;
    private List<Statement> statementList;
    private StatementProfile statementCategory;
}
