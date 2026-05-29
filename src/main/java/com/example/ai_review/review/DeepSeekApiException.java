package com.example.ai_review.review;

public class DeepSeekApiException extends RuntimeException {

    public DeepSeekApiException(String message) {
        super(message);
    }

    public DeepSeekApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
