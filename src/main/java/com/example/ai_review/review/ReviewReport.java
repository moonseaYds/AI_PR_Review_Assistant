package com.example.ai_review.review;

import java.util.List;

public record ReviewReport(
        String summary,
        RiskLevel riskLevel,
        List<RiskItem> risks,
        List<SuggestionItem> suggestions,
        String model
) {
}
