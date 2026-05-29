package com.example.ai_review.diff;

import java.util.List;

/**
 * Structured diff context consumed by downstream AI Review.
 */
public record DiffReviewContext(
        String owner,
        String repo,
        int pullNumber,
        String title,
        int totalFiles,
        int totalAdditions,
        int totalDeletions,
        int totalChanges,
        boolean truncated,
        String truncationReason,
        List<FileContext> fileContexts
) {
}
