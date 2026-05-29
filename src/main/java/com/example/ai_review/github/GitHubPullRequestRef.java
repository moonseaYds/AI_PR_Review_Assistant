package com.example.ai_review.github;

public record GitHubPullRequestRef(
        String owner,
        String repo,
        int pullNumber,
        String normalizedUrl
) {
}
