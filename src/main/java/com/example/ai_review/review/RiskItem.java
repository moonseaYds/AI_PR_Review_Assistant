package com.example.ai_review.review;

public record RiskItem(
        String file,
        String level,
        String title,
        String reason,
        String suggestion
) {
}
