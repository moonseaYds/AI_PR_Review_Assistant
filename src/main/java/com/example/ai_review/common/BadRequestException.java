package com.example.ai_review.common;

public class BadRequestException extends RuntimeException {

    private final ErrorCode code;
    private final String suggestion;

    public BadRequestException(ErrorCode code, String message, String suggestion) {
        super(message);
        this.code = code;
        this.suggestion = suggestion;
    }

    public ErrorCode getCode() { return code; }
    public String getSuggestion() { return suggestion; }
}
