package com.example.ai_review.report;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewAnalysisController {

    private final ReviewAnalysisService analysisService;

    public ReviewAnalysisController(ReviewAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public AnalyzePullRequestResponse analyze(@Valid @RequestBody AnalyzePullRequestRequest request) {
        return analysisService.analyze(request.prUrl());
    }
}
