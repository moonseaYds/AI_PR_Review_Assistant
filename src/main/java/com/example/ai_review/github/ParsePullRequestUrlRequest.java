package com.example.ai_review.github;

import jakarta.validation.constraints.NotBlank;

public record ParsePullRequestUrlRequest(
        @NotBlank(message = "不能为空")
        String prUrl
) {
}
