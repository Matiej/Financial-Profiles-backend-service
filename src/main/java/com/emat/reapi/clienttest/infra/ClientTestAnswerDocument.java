package com.emat.reapi.clienttest.infra;

import com.emat.reapi.clienttest.domain.ClientTestAnswer;
import com.emat.reapi.statement.domain.StatementProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientTestAnswerDocument {
    private String questionKey;
    private StatementProfile category;
    private String limitingDescription;
    private String supportingDescription;
    private int scoring;

    public ClientTestAnswer toDomain() {
        return new ClientTestAnswer(questionKey, category, limitingDescription, supportingDescription, scoring);
    }
}
