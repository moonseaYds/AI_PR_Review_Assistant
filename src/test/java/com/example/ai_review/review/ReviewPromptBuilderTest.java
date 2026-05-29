package com.example.ai_review.review;

import com.example.ai_review.diff.DiffReviewContext;
import com.example.ai_review.diff.FileContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewPromptBuilderTest {

    private final ReviewPromptBuilder builder = new ReviewPromptBuilder();

    @Test
    void systemPromptIncludesReviewDimensions() {
        String prompt = builder.buildSystemPrompt();

        assertTrue(prompt.contains("正确性"));
        assertTrue(prompt.contains("空指针"));
        assertTrue(prompt.contains("安全性"));
        assertTrue(prompt.contains("性能"));
        assertTrue(prompt.contains("JSON"));
        assertTrue(prompt.contains("riskLevel"));
        assertTrue(prompt.contains("risks"));
        assertTrue(prompt.contains("suggestions"));
        assertTrue(prompt.contains("HIGH"));
        assertTrue(prompt.contains("MEDIUM"));
        assertTrue(prompt.contains("LOW"));
    }

    @Test
    void userPromptContainsPrInfo() {
        DiffReviewContext context = new DiffReviewContext(
                "owner", "repo", 123, "Fix bug", 1, 10, 3, 13,
                false, null,
                List.of(new FileContext("App.java", "modified", 10, 3, 13,
                        "@@ -1 +1 @@ hello", false))
        );

        String prompt = builder.buildUserPrompt(context);

        assertTrue(prompt.contains("owner/repo"));
        assertTrue(prompt.contains("#123"));
        assertTrue(prompt.contains("Fix bug"));
        assertTrue(prompt.contains("10"));
        assertTrue(prompt.contains("3"));
        assertTrue(prompt.contains("13"));
    }

    @Test
    void userPromptContainsFilePatchExcerpt() {
        DiffReviewContext context = new DiffReviewContext(
                "o", "r", 1, "PR", 1, 1, 0, 1,
                false, null,
                List.of(new FileContext("src/main/App.java", "added", 1, 0, 1,
                        "+public class App {}", false))
        );

        String prompt = builder.buildUserPrompt(context);

        assertTrue(prompt.contains("src/main/App.java"));
        assertTrue(prompt.contains("added"));
        assertTrue(prompt.contains("+public class App {}"));
    }

    @Test
    void userPromptMarksTruncation() {
        DiffReviewContext context = new DiffReviewContext(
                "o", "r", 1, "Large PR", 5, 500, 200, 700,
                true, "超过总长度限制",
                List.of(new FileContext("Big.java", "modified", 500, 200, 700,
                        "long patch...", true))
        );

        String prompt = builder.buildUserPrompt(context);

        assertTrue(prompt.contains("截断"));
        assertTrue(prompt.contains("超过总长度限制"));
    }

    @Test
    void userPromptContainsMultipleFiles() {
        FileContext f1 = new FileContext("A.java", "modified", 3, 1, 4,
                "@@ diff a", false);
        FileContext f2 = new FileContext("B.java", "added", 10, 0, 10,
                "@@ diff b", false);

        DiffReviewContext context = new DiffReviewContext(
                "o", "r", 1, "Multi-file PR", 2, 13, 1, 14,
                false, null,
                List.of(f1, f2)
        );

        String prompt = builder.buildUserPrompt(context);

        assertTrue(prompt.contains("A.java"));
        assertTrue(prompt.contains("B.java"));
        assertTrue(prompt.contains("@@ diff a"));
        assertTrue(prompt.contains("@@ diff b"));
    }

    @Test
    void promptDoesNotContainApiKey() {
        DiffReviewContext context = new DiffReviewContext(
                "o", "r", 1, "PR", 1, 1, 0, 1,
                false, null,
                List.of(new FileContext("App.java", "modified", 1, 0, 1,
                        "patch", false))
        );

        String systemPrompt = builder.buildSystemPrompt();
        String userPrompt = builder.buildUserPrompt(context);

        assertFalse(systemPrompt.contains("sk-"));
        assertFalse(userPrompt.contains("sk-"));
        assertFalse(systemPrompt.contains("Bearer"));
        assertFalse(userPrompt.contains("Bearer"));
    }
}
