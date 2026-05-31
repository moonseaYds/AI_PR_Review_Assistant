package com.example.ai_review.report;

import com.example.ai_review.review.RiskLevel;

import java.util.List;

public record MergeRiskReport(
        RiskLevel riskLevel,
        String summary,
        List<MergeRiskItem> items
) {
}
