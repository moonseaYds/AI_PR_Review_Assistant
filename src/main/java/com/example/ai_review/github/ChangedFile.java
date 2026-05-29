package com.example.ai_review.github;

/**
 * Represents a changed file in the PR fetch result.
 */
public record ChangedFile(
        String filename,
        String status,
        int additions,
        int deletions,
        int changes,
        String patch
) {
}
