package com.example.ai_review.review;

import java.util.List;

record DeepSeekChatResponse(
        List<Choice> choices
) {
    record Choice(Message message) {
    }

    record Message(String content) {
    }
}
