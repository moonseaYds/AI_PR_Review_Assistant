package com.example.ai_review.github;

import com.example.ai_review.diff.BuildDiffContextRequest;
import com.example.ai_review.diff.DiffContextBuilder;
import com.example.ai_review.diff.DiffReviewContext;
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
    private final DiffContextBuilder diffContextBuilder;

    public PullRequestParseController(GitHubPullRequestUrlParser parser,
                                      GitHubPrFetcher fetcher,
                                      DiffContextBuilder diffContextBuilder) {
        this.parser = parser;
        this.fetcher = fetcher;
        this.diffContextBuilder = diffContextBuilder;
    }

    @PostMapping("/parse-pr-url")
    public GitHubPullRequestRef parsePullRequestUrl(@Valid @RequestBody ParsePullRequestUrlRequest request) {
        return parser.parse(request.prUrl());
    }

    @PostMapping("/fetch-pr")
    public PrFetchResult fetchPullRequest(@Valid @RequestBody FetchPullRequestRequest request) {
        GitHubPullRequestRef ref = parser.parse(request.prUrl());
        return fetcher.fetch(ref, request.credentials());
    }

    @PostMapping("/build-diff-context")
    public DiffReviewContext buildDiffContext(@Valid @RequestBody BuildDiffContextRequest request) {
        return diffContextBuilder.build(request);
    }
}
