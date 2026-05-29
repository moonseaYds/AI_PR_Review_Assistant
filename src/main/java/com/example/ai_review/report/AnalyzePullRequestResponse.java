package com.example.ai_review.report;

import com.example.ai_review.review.ReviewReport;

public record AnalyzePullRequestResponse(
        String owner,
        String repo,
        int pullNumber,
        String title,
        String author,
        String state,
        String baseBranch,
        String headBranch,
        int totalFiles,
        int totalAdditions,
        int totalDeletions,
        int totalChanges,
        boolean truncated,
        String truncationReason,
        ReviewReport review
) {
}
