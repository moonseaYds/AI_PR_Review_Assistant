package com.example.ai_review.review;

public record SuggestionItem(
        String file,
        String category,
        String content,
        Integer lineNumber,
        String codeSnippet,
        String exampleFix
) {
}
