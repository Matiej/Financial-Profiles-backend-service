package com.emat.reapi.tally;

import com.emat.reapi.api.dto.ClientAnswerDto;
import com.emat.reapi.api.tally.TallyWebhookEvent;
import com.emat.reapi.clienttalytest.ClientAnswerService;
import com.emat.reapi.statement.port.StatementDefinitionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class TallyServiceImpl implements TallyService {
    private final ClientAnswerService clientAnswerService;
    private final StatementDefinitionService definitionService;

    @Override
    public Mono<Void> processTallyEvent(TallyWebhookEvent event) {
        return definitionService.getAllStatementDefinitions()
                .collectList()
                .flatMap(statementDefinitions -> {
                    ClientAnswerDto clientAnswerDto = TallyToDtoMapper.map(event, statementDefinitions);
                    return clientAnswerService.saveClientAnswers(clientAnswerDto);
                }).then();
    }
}
