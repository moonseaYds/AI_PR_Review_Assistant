package com.example.ai_review.review;

import com.example.ai_review.common.RuntimeCredentials;

public interface AiReviewModelClient {

    String chat(String systemPrompt, String userPrompt);

    default String chat(String systemPrompt, String userPrompt, RuntimeCredentials credentials) {
        return chat(systemPrompt, userPrompt);
    }

    ReviewReport parseReviewReport(String jsonContent);

    String getModel();

    String getProviderName();
}
