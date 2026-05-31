package com.example.ai_review.report;

import com.example.ai_review.common.RuntimeCredentials;
import com.example.ai_review.diff.AnalysisMode;
import jakarta.validation.constraints.NotBlank;

public record AnalyzePullRequestRequest(
        @NotBlank(message = "不能为空")
        String prUrl,
        AnalysisMode analysisMode,
        RuntimeCredentials credentials
) {

    public AnalyzePullRequestRequest(String prUrl, AnalysisMode analysisMode) {
        this(prUrl, analysisMode, RuntimeCredentials.empty());
    }
}
