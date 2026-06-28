package com.emat.reapi.api.dto.InsightReportDto;

import com.emat.reapi.profileanalysis.domain.reportjob.ReportJob;
import com.emat.reapi.profileanalysis.domain.reportjob.ReportJobStatus;

import java.time.Duration;
import java.time.Instant;

public record ReportJobStatusDto(
        String submissionId,
        String status,
        String mode,
        String error,
        Instant createdAt,
        Instant updatedAt,
        Instant expireAt,
        boolean isLocked,
        Long remainingLockSeconds
) {
    public static ReportJobStatusDto from(ReportJob job, Instant now) {
        boolean isJobActive = job.getStatus() == ReportJobStatus.PENDING || job.getStatus() == ReportJobStatus.RUNNING;
        boolean isTimeLock = job.getExpireAt() != null && now.isBefore(job.getExpireAt());
        boolean isAnalyzeButtonLocked = isJobActive || isTimeLock;

        Long remain = null;
        if (isAnalyzeButtonLocked && job.getExpireAt() != null) {
            long secs = Duration.between(now, job.getExpireAt()).getSeconds();
            remain = Math.max(0, secs);
        }

        return new ReportJobStatusDto(
                job.getSubmissionId(),
                job.getStatus().name(),
                job.getMode() == null ? null : job.getMode().name(),
                job.getError(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getExpireAt(),
                isAnalyzeButtonLocked,
                remain
        );
    }
}
