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
        AnalysisMode analysisMode,
        String contextStrategy,
        List<FileContext> fileContexts
) {
    public DiffReviewContext(String owner,
                             String repo,
                             int pullNumber,
                             String title,
                             int totalFiles,
                             int totalAdditions,
                             int totalDeletions,
                             int totalChanges,
                             boolean truncated,
                             String truncationReason,
                             List<FileContext> fileContexts) {
        this(owner, repo, pullNumber, title, totalFiles, totalAdditions, totalDeletions, totalChanges,
                truncated, truncationReason, AnalysisMode.FAST, "FAST 模式：优先保留高风险文件上下文", fileContexts);
    }
}
