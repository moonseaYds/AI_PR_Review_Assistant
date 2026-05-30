package com.example.ai_review.github;

public record PublishPullRequestCommentResponse(
        String owner,
        String repo,
        int pullNumber,
        String commentUrl
) {
}
