package com.example.ai_review.github;

import com.example.ai_review.common.RuntimeCredentials;
import jakarta.validation.constraints.NotBlank;

public record FetchPullRequestRequest(
        @NotBlank(message = "不能为空")
        String prUrl,
        RuntimeCredentials credentials
) {

    public FetchPullRequestRequest(String prUrl) {
        this(prUrl, RuntimeCredentials.empty());
    }
}
