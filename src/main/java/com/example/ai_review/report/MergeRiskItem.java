package com.example.ai_review.report;

public record MergeRiskItem(
        String category,
        String file,
        String level,
        String reason,
        String suggestion
) {
}
