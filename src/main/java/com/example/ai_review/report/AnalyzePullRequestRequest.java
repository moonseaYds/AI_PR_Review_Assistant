package com.example.ai_review.report;

import jakarta.validation.constraints.NotBlank;

public record AnalyzePullRequestRequest(
        @NotBlank(message = "不能为空")
        String prUrl
) {
}
