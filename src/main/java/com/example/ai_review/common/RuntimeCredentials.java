package com.example.ai_review.common;

public record RuntimeCredentials(
        String deepSeekApiKey,
        String githubToken
) {

    public static RuntimeCredentials empty() {
        return new RuntimeCredentials(null, null);
    }

    public String normalizedDeepSeekApiKey() {
        return normalize(deepSeekApiKey);
    }

    public String normalizedGitHubToken() {
        return normalize(githubToken);
    }

    private String normalize(String value) {
        return value != null ? value.strip() : "";
    }
}
