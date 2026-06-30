package com.emat.reapi.fptest.domain;

import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementType;
import com.emat.reapi.statement.domain.StatementTypeDefinition;

import java.util.Map;

public record FpTestStatement(
        String statementKey,
        String statementsDescription,
        String statementsCategory
) {

    // statementsCategory carries the live profile name (plName), resolved from profileNamesById;
    // falls back to the raw profileId when the profile is missing (e.g. soft-deleted).
    public static FpTestStatement formStatementDefinition(StatementDefinition statementDefinition,
                                                          Map<String, String> profileNamesById) {
        var limitingDesc = descriptionFor(statementDefinition, StatementType.LIMITING);
        var supportingDesc = descriptionFor(statementDefinition, StatementType.SUPPORTING);
        var desc = (limitingDesc + "-" + supportingDesc).trim();
        String profileId = statementDefinition.getProfileId();
        return new FpTestStatement(
                statementDefinition.getStatementKey(),
                desc,
                profileNamesById.getOrDefault(profileId, profileId)
        );
    }

    private static String descriptionFor(StatementDefinition definition, StatementType type) {
        return definition.getStatementTypeDefinitions()
                .stream()
                .filter(def -> def.getStatementType() == type)
                .map(StatementTypeDefinition::getStatementDescription)
                .findFirst()
                .orElse("");
    }

}
