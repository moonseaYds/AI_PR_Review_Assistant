package com.example.ai_review.review;

import com.example.ai_review.common.ErrorCode;

public class DeepSeekApiException extends RuntimeException {

    private final ErrorCode code;
    private final String suggestion;
    private final boolean retryable;

    public DeepSeekApiException(ErrorCode code, String message, String suggestion, boolean retryable) {
        super(message);
        this.code = code;
        this.suggestion = suggestion;
        this.retryable = retryable;
    }

    public DeepSeekApiException(String message) {
        this(ErrorCode.DEEPSEEK_UPSTREAM_ERROR, message, null, true);
    }

    public DeepSeekApiException(ErrorCode code, String message, String suggestion,
                                  boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.suggestion = suggestion;
        this.retryable = retryable;
    }

    public DeepSeekApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.DEEPSEEK_UPSTREAM_ERROR;
        this.suggestion = null;
        this.retryable = true;
    }

    public ErrorCode getCode() { return code; }
    public String getSuggestion() { return suggestion; }
    public boolean isRetryable() { return retryable; }
}
