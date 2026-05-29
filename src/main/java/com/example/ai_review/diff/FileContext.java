package com.example.ai_review.diff;

/**
 * Per-file context in a diff review, with optional patch truncation.
 */
public record FileContext(
        String filename,
        String status,
        int additions,
        int deletions,
        int changes,
        String patchExcerpt,
        boolean patchTruncated
) {
}
