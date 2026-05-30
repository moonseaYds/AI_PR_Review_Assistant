package com.example.ai_review.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        String suggestion,
        Boolean retryable,
        String timestamp
) {

    public static ApiError badRequest(String message) {
        return new ApiError(ErrorCode.BAD_REQUEST.name(), message, null, false, Instant.now().toString());
    }

    public static ApiError badRequest(ErrorCode code, String message) {
        return new ApiError(code.name(), message, null, false, Instant.now().toString());
    }

    public static ApiError upstreamError(ErrorCode code, String message,
                                          String suggestion, boolean retryable) {
        return new ApiError(code.name(), message, suggestion, retryable, Instant.now().toString());
    }

    // Backward-compatible factory
    public static ApiError upstreamError(String message) {
        return new ApiError(ErrorCode.GITHUB_UPSTREAM_ERROR.name(), message, null, true,
                Instant.now().toString());
    }
}
