package com.emat.reapi.tally;

import com.emat.reapi.api.dto.AnsweredStatementDto;
import com.emat.reapi.api.dto.ClientAnswerDto;
import com.emat.reapi.api.dto.StatementDto;
import com.emat.reapi.api.tally.TallyWebhookEvent;
import com.emat.reapi.statement.domain.StatementDefinition;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public final class TallyToDtoMapper {
    private TallyToDtoMapper() {
    }

    public static ClientAnswerDto map(TallyWebhookEvent event, List<StatementDefinition> statementDefinitions) {
        var data = event.getData();
        String clientId = data.getRespondentId();
        String submissionId = data.getSubmissionId();
        Instant submissionDate = Instant.parse(data.getCreatedAt());

        String name = findValueByLabel(data.getFields(), "Imię");

        Map<String, AnsweredStatementDto> resultMap = new LinkedHashMap<>();

        for (var field : data.getFields()) {
            if (!"MULTIPLE_CHOICE".equalsIgnoreCase(field.getType())) continue;

            String key = field.getKey();

            final StatementDefinition def = statementDefinitions
                    .stream()
                    .filter(p -> key.equalsIgnoreCase(p.getStatementKey()))
                    .findAny()
                    .orElse(null);

            if (def == null) {
                log.warn("No definition for key={}", key);
                continue;
            }

            List<String> selectedIds = Collections.emptyList();
            Object valueObj = field.getValue();
            if (valueObj instanceof List<?> list) {
                selectedIds = list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
            } else if (valueObj instanceof String s) {
                selectedIds = List.of(s);
            } else if (valueObj != null) {
                log.warn("Unexpected MULTIPLE_CHOICE value type for key {}: {}", key, valueObj.getClass());
            }

            AnsweredStatementDto answered = resultMap.computeIfAbsent(key, k -> {
                List<StatementDto> allOptions = def.getStatementTypeDefinitions().stream()
                        .map(opt -> new StatementDto(
                                opt.getKey(),
                                opt.getStatementDescription(),
                                null // status uzupełnimy poniżej
                        ))
                        .collect(Collectors.toList());
                return new AnsweredStatementDto(def.getStatementId(), key, allOptions);
            });

            Set<String> selectedSet = new HashSet<>(selectedIds);
            for (String sel : selectedSet) {
                boolean known = def.getStatementTypeDefinitions().stream()
                        .anyMatch(opt -> Objects.equals(opt.getKey(), sel));
                if (!known) {
                    log.warn("Selected optionId {} not present in definition for key {}", sel, key);
                }
            }

            List<StatementDto> updated = answered.getStatementDtoList().stream()
                    .map(s -> new StatementDto(
                            s.statementKey(),
                            s.statementDescription(),
                            selectedSet.contains(s.statementKey())
                    ))
                    .toList();

            answered.getStatementDtoList().clear();
            answered.getStatementDtoList().addAll(updated);
        }

        List<AnsweredStatementDto> answeredStatements = new ArrayList<>(resultMap.values());
        answeredStatements.sort(Comparator.comparingInt(dto -> Integer.parseInt(dto.getStatementId())));

        return new ClientAnswerDto(
                clientId,
                submissionId,
                submissionDate,
                name,
                data.getFormName(),
                answeredStatements
        );
    }

    private static String findValueByLabel(List<TallyWebhookEvent.Field> fields, String label) {
        if (fields == null) return null;
        for (var f : fields) {
            if (label.equalsIgnoreCase(f.getLabel())) {
                Object v = f.getValue();
                return v == null ? null : String.valueOf(v);
            }
        }
        return null;
    }
}
