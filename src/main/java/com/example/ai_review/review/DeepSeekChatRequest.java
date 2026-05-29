package com.example.ai_review.review;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record DeepSeekChatRequest(
        String model,
        List<Message> messages,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens,
        @JsonProperty("response_format") ResponseFormat responseFormat
) {
    record Message(String role, String content) {
    }

    record ResponseFormat(String type) {
    }
}
