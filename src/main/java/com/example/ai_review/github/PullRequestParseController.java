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
    private final GitHubPrFetcher fetcher;

    public PullRequestParseController(GitHubPullRequestUrlParser parser, GitHubPrFetcher fetcher) {
        this.parser = parser;
        this.fetcher = fetcher;
    }

    @PostMapping("/parse-pr-url")
    public GitHubPullRequestRef parsePullRequestUrl(@Valid @RequestBody ParsePullRequestUrlRequest request) {
        return parser.parse(request.prUrl());
    }

    @PostMapping("/fetch-pr")
    public PrFetchResult fetchPullRequest(@Valid @RequestBody FetchPullRequestRequest request) {
        GitHubPullRequestRef ref = parser.parse(request.prUrl());
        return fetcher.fetch(ref);
    }
}
