package com.example.ai_review.diff;

import com.example.ai_review.common.RuntimeCredentials;
import jakarta.validation.constraints.NotBlank;

public record AnalyzeDiffRequest(
        String repository,
        String baseBranch,
        String headBranch,
        AnalysisMode analysisMode,
        RuntimeCredentials credentials,
        @NotBlank(message = "diffText 不能为空")
        String diffText
) {
    public AnalyzeDiffRequest(String repository, String baseBranch, String headBranch, String diffText) {
        this(repository, baseBranch, headBranch, AnalysisMode.FAST, RuntimeCredentials.empty(), diffText);
    }

    public AnalyzeDiffRequest(String repository,
                              String baseBranch,
                              String headBranch,
                              AnalysisMode analysisMode,
                              String diffText) {
        this(repository, baseBranch, headBranch, analysisMode, RuntimeCredentials.empty(), diffText);
    }
}
