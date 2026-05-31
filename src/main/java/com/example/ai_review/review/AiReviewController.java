package com.example.ai_review.review;

import com.example.ai_review.diff.DiffReviewContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class AiReviewController {

    private final AiReviewModelClient modelClient;
    private final ReviewPromptBuilder promptBuilder;

    public AiReviewController(AiReviewModelClient modelClient, ReviewPromptBuilder promptBuilder) {
        this.modelClient = modelClient;
        this.promptBuilder = promptBuilder;
    }

    @PostMapping("/ai-review")
    public ReviewReport aiReview(@Valid @RequestBody DiffReviewContext request) {
        if (request.fileContexts() == null || request.fileContexts().isEmpty()) {
            throw new IllegalArgumentException("fileContexts 不能为空，请先调用 /api/reviews/build-diff-context 构造上下文");
        }

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(request);

        String rawResponse = modelClient.chat(systemPrompt, userPrompt);
        ReviewReport report = modelClient.parseReviewReport(rawResponse);

        return new ReviewReport(
                report.summary(),
                report.riskLevel(),
                report.risks() != null ? report.risks() : java.util.List.of(),
                report.suggestions() != null ? report.suggestions() : java.util.List.of(),
                modelClient.getModel()
        );
    }
}
