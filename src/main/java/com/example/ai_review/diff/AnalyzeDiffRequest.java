package com.example.ai_review.diff;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeDiffRequest(
        String repository,
        String baseBranch,
        String headBranch,
        AnalysisMode analysisMode,
        @NotBlank(message = "diffText 不能为空")
        String diffText
) {
    public AnalyzeDiffRequest(String repository, String baseBranch, String headBranch, String diffText) {
        this(repository, baseBranch, headBranch, AnalysisMode.FAST, diffText);
    }
}
