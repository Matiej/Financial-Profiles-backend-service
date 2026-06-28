package com.emat.reapi.statement.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class StatementDefinition {
    private String statementId;
    private String profileId;
    private String statementKey;
    private List<StatementTypeDefinition> statementTypeDefinitions;

    // Transitional bridge: downstream (ClientTest, FpTest) still consumes the StatementProfile
    // enum until F6. Resolves the enum from the stored profileId. Removed once the enum is gone.
    public StatementProfile getCategory() {
        return StatementProfile.fromProfileId(profileId);
    }
}
