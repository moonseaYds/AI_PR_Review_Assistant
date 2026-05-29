package com.example.ai_review.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps fields from GitHub REST API GET /repos/{owner}/{repo}/pulls/{pullNumber}/files.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPrFileResponse(
        String filename,
        String status,
        int additions,
        int deletions,
        int changes,
        String patch
) {
}
