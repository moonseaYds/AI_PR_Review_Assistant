package com.example.ai_review.report;

import com.example.ai_review.diff.AnalysisMode;
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
        AnalysisMode analysisMode,
        String contextStrategy,
        ReviewReport review
) {
    public AnalyzePullRequestResponse(String owner,
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
                                      ReviewReport review) {
        this(owner, repo, pullNumber, title, author, state, baseBranch, headBranch, totalFiles,
                totalAdditions, totalDeletions, totalChanges, truncated, truncationReason,
                AnalysisMode.FAST, "FAST 模式：优先保留高风险文件上下文", review);
    }
}
