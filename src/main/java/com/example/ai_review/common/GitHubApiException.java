package com.example.ai_review.common;

public class GitHubApiException extends RuntimeException {

    private final ErrorCode code;
    private final String suggestion;
    private final boolean retryable;

    public GitHubApiException(ErrorCode code, String message, String suggestion, boolean retryable) {
        super(message);
        this.code = code;
        this.suggestion = suggestion;
        this.retryable = retryable;
    }

    public GitHubApiException(String message) {
        this(ErrorCode.GITHUB_UPSTREAM_ERROR, message, null, true);
    }

    public GitHubApiException(ErrorCode code, String message, String suggestion,
                                boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.suggestion = suggestion;
        this.retryable = retryable;
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.GITHUB_UPSTREAM_ERROR;
        this.suggestion = null;
        this.retryable = true;
    }

    public ErrorCode getCode() { return code; }
    public String getSuggestion() { return suggestion; }
    public boolean isRetryable() { return retryable; }
}
