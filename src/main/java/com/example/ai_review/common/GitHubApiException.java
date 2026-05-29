package com.example.ai_review.common;

/**
 * Thrown when the GitHub API returns a non-success response or a network error occurs.
 */
public class GitHubApiException extends RuntimeException {

    public GitHubApiException(String message) {
        super(message);
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
