package com.example.ai_review.diff;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeDiffRequest(
        String repository,
        String baseBranch,
        String headBranch,
        @NotBlank(message = "diffText 不能为空")
        String diffText
) {
}
