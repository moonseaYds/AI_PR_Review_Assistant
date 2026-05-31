package com.example.ai_review.review;

public interface AiReviewModelClient {

    String chat(String systemPrompt, String userPrompt);

    ReviewReport parseReviewReport(String jsonContent);

    String getModel();

    String getProviderName();
}
