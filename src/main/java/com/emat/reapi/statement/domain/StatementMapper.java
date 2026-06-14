package com.emat.reapi.statement.domain;

import com.emat.reapi.api.dto.AnsweredStatementDto;
import com.emat.reapi.api.dto.AnsweredStatementResponse;
import com.emat.reapi.api.dto.StatementDto;
import com.emat.reapi.api.dto.StatementResponse;
import com.emat.reapi.clienttalytest.domain.ClientStatement;

import java.util.List;

public final class StatementMapper {
    private StatementMapper() {
    }

    public static ClientStatement toDomain(AnsweredStatementDto dto, List<StatementDefinition> statementDefinitionDocuments) {
        if (dto == null) return null;
        return statementDefinitionDocuments
                .stream()
                .filter(p -> p.getStatementId().equalsIgnoreCase(dto.getStatementId()))
                .findFirst()
                .map(definition ->
                        new ClientStatement(
                                definition.getStatementId(),
                                definition.getStatementKey(),
                                toStatements(definition.getStatementTypeDefinitions(), dto.getStatementDtoList()),
                                definition.getCategory())
                ).orElseThrow(() -> new IllegalArgumentException("Can't find definition for statementId: " + dto.getStatementId()));
    }

    public static AnsweredStatementResponse toAnsweredStatementResponse(ClientStatement clientStatement) {
        return new AnsweredStatementResponse(
                clientStatement.getStatementId(),
                clientStatement.getKey(),
                clientStatement.getStatementCategory().getPlName(),
                clientStatement.getStatementList().stream().map(StatementMapper::toStatementResponseDto).toList()
        );
    }

    private static StatementResponse toStatementResponseDto(Statement statement) {
        return new StatementResponse(
                statement.getStatementKey(),
                statement.getStatementDescription(),
                statement.getStatementStatus(),
                statement.getStatementType().getPlDescription()
        );
    }

    @Deprecated
    private static List<Statement> toStatements(List<StatementTypeDefinition> statementDefinitions,
                                                List<StatementDto> dto) {
        return statementDefinitions.stream()
                .map(def -> new Statement(
                        def.getStatementType().name(),
                        def.getStatementDescription(),
                        def.getStatementType(),
                        dto.stream()
                                .filter(p -> p.statementKey().equalsIgnoreCase(def.getStatementType().name()))
                                .map(StatementDto::status)
                                .findAny()
                                .orElse(Boolean.FALSE)
                )).toList();
    }
}
