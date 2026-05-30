package com.example.ai_review.report;

import com.example.ai_review.diff.AnalyzeDiffRequest;
import com.example.ai_review.github.GitHubPrCommentPublisher;
import com.example.ai_review.github.GitHubPullRequestRef;
import com.example.ai_review.github.GitHubPullRequestUrlParser;
import com.example.ai_review.github.PublishPullRequestCommentRequest;
import com.example.ai_review.github.PublishPullRequestCommentResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewAnalysisController {

    private final ReviewAnalysisService analysisService;
    private final GitHubPullRequestUrlParser parser;
    private final GitHubPrCommentPublisher commentPublisher;
    private final ReviewCommentFormatter commentFormatter;

    public ReviewAnalysisController(ReviewAnalysisService analysisService,
                                     GitHubPullRequestUrlParser parser,
                                     GitHubPrCommentPublisher commentPublisher,
                                     ReviewCommentFormatter commentFormatter) {
        this.analysisService = analysisService;
        this.parser = parser;
        this.commentPublisher = commentPublisher;
        this.commentFormatter = commentFormatter;
    }

    @PostMapping("/analyze")
    public AnalyzePullRequestResponse analyze(@Valid @RequestBody AnalyzePullRequestRequest request) {
        return analysisService.analyze(request.prUrl());
    }

    @PostMapping("/analyze-diff")
    public AnalyzePullRequestResponse analyzeDiff(@Valid @RequestBody AnalyzeDiffRequest request) {
        return analysisService.analyzeDiff(request);
    }

    @PostMapping("/publish-comment")
    public PublishPullRequestCommentResponse publishComment(
            @Valid @RequestBody PublishPullRequestCommentRequest request) {
        GitHubPullRequestRef ref = parser.parse(request.prUrl());
        String markdown = commentFormatter.format(request.analysis());
        return commentPublisher.publish(ref, markdown);
    }
}
