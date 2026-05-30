package com.example.ai_review.diff;

public enum AnalysisMode {
    FAST,
    DEEP;

    public static AnalysisMode defaultIfNull(AnalysisMode mode) {
        return mode != null ? mode : FAST;
    }
}
