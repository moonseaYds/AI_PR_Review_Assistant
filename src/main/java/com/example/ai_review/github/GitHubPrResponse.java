package com.example.ai_review.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps fields from GitHub REST API GET /repos/{owner}/{repo}/pulls/{pullNumber}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPrResponse(
        String title,
        User user,
        String state,
        Base base,
        Head head
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            String login
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Base(
            @JsonProperty("ref") String ref
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Head(
            @JsonProperty("ref") String ref
    ) {
    }
}
