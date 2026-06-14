package com.emat.reapi.profiler.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ScoringProfileBlock {
    private String profileId;
    private String profileName;
    private String computedLabel;
    private double scorePercent;
    private int totalAnswers;
    private int totalScore;
    private double avgScore;
    private List<ScoringStatementPair> answersBySeverity;
}
