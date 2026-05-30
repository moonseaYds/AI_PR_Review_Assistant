package com.example.ai_review.report;

import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskItem;
import com.example.ai_review.review.RiskLevel;
import com.example.ai_review.review.SuggestionItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCommentFormatterTest {

    private final ReviewCommentFormatter formatter = new ReviewCommentFormatter();

    @Test
    void formatsFullAnalysisResponse() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "owner", "repo", 1, "Fix bug", "octocat", "open",
                "main", "feature/fix", 3, 10, 2, 12, false, null,
                new ReviewReport("代码质量良好", RiskLevel.LOW,
                        List.of(new RiskItem("App.java", "LOW", "命名建议",
                                "变量不清晰", "改为 userName")),
                        List.of(new SuggestionItem("App.java", "可维护性", "加测试")),
                        "deepseek-v4-flash")
        );

        String md = formatter.format(response);

        assertTrue(md.contains("AI PR Review Assistant"));
        assertTrue(md.contains("owner/repo"));
        assertTrue(md.contains("#1 Fix bug"));
        assertTrue(md.contains("octocat"));
        assertTrue(md.contains("feature/fix → main"));
        assertTrue(md.contains("3 个文件"));
        assertTrue(md.contains("代码质量良好"));
        assertTrue(md.contains("LOW"));
        assertTrue(md.contains("命名建议"));
        assertTrue(md.contains("App.java"));
        assertTrue(md.contains("加测试"));
        assertTrue(md.contains("不替代人工 Code Review"));
    }

    @Test
    void formatsEmptyRisks() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 2, "Clean PR", "dev", "open",
                "main", "feat", 1, 5, 0, 5, false, null,
                new ReviewReport("未发现风险", RiskLevel.LOW,
                        List.of(), List.of(), "deepseek-v4-flash")
        );

        String md = formatter.format(response);

        assertTrue(md.contains("风险等级"));
        assertTrue(md.contains("LOW"));
        assertFalse(md.contains("风险点"));
        assertFalse(md.contains("Review 建议"));
    }

    @Test
    void formatsTruncatedPR() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 3, "Big PR", "dev", "open",
                "main", "feat", 10, 500, 100, 600, true, "超过总长度限制",
                new ReviewReport("大型 PR", RiskLevel.HIGH,
                        List.of(), List.of(), "deepseek-v4-flash")
        );

        String md = formatter.format(response);

        assertTrue(md.contains("截断"));
        assertTrue(md.contains("超过总长度限制"));
    }

    @Test
    void handlesNullFieldsGracefully() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 4, "Title", null, null,
                null, null, 0, 0, 0, 0, false, null,
                new ReviewReport(null, null, null, null, null)
        );

        String md = formatter.format(response);
        assertNotNull(md);
        assertFalse(md.contains("null"));
    }

    @Test
    void doesNotContainRealTokens() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 5, "PR", "dev", "open",
                "main", "feat", 1, 1, 0, 1, false, null,
                new ReviewReport("OK", RiskLevel.LOW,
                        List.of(), List.of(), "deepseek-v4-flash")
        );

        String md = formatter.format(response);

        assertFalse(md.contains("sk-"));
        assertFalse(md.contains("ghp_"));
        assertFalse(md.contains("Bearer"));
    }
}
