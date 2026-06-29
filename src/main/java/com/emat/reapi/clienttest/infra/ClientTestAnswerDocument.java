package com.emat.reapi.clienttest.infra;

import com.emat.reapi.clienttest.domain.ClientTestAnswer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientTestAnswerDocument {
    private String questionKey;
    private String profileId;
    private String limitingDescription;
    private String supportingDescription;
    private int scoring;

    public ClientTestAnswer toDomain() {
        return new ClientTestAnswer(questionKey, profileId, limitingDescription, supportingDescription, scoring);
    }
}
