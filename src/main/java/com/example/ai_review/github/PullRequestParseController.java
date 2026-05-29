package com.example.ai_review.github;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class PullRequestParseController {

    private final GitHubPullRequestUrlParser parser;

    public PullRequestParseController(GitHubPullRequestUrlParser parser) {
        this.parser = parser;
    }

    @PostMapping("/parse-pr-url")
    public GitHubPullRequestRef parsePullRequestUrl(@Valid @RequestBody ParsePullRequestUrlRequest request) {
        return parser.parse(request.prUrl());
    }
}
