package com.example.ai_review.github;

import java.util.List;

/**
 * Response DTO for POST /api/reviews/fetch-pr.
 */
public record PrFetchResult(
        String owner,
        String repo,
        int pullNumber,
        String title,
        String author,
        String state,
        String baseBranch,
        String headBranch,
        List<ChangedFile> changedFiles
) {
}
