package com.example.ai_review.github;

import com.example.ai_review.common.RuntimeCredentials;
import com.example.ai_review.report.AnalyzePullRequestResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PublishPullRequestCommentRequest(
        @NotBlank(message = "不能为空")
        String prUrl,

        @NotNull(message = "分析结果不能为空")
        @Valid
        AnalyzePullRequestResponse analysis,

        RuntimeCredentials credentials
) {

    public PublishPullRequestCommentRequest(String prUrl, AnalyzePullRequestResponse analysis) {
        this(prUrl, analysis, RuntimeCredentials.empty());
    }
}
