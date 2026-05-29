package com.example.ai_review.common;

import java.time.Instant;

public record ApiError(
        String code,
        String message,
        String timestamp
) {

    public static ApiError badRequest(String message) {
        return new ApiError("BAD_REQUEST", message, Instant.now().toString());
    }

    public static ApiError upstreamError(String message) {
        return new ApiError("UPSTREAM_ERROR", message, Instant.now().toString());
    }
}
