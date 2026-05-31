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
                                "变量不清晰", "改为 userName",
                                12, "String x = getValue();", "String userName = getValue();")),
                        List.of(new SuggestionItem("App.java", "可维护性", "加测试",
                                20, "// TODO refactor", "// 已重构")),
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
        // 代码证据字段
        assertTrue(md.contains("第 12 行附近"));
        assertTrue(md.contains("String x = getValue();"));
        assertTrue(md.contains("String userName = getValue();"));
        assertTrue(md.contains("第 20 行附近"));
        assertTrue(md.contains("// TODO refactor"));
        assertTrue(md.contains("// 已重构"));
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
    void formatsMergeRiskReport() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 6, "Risk PR", "dev", "open",
                "main", "feat", 2, 20, 3, 23, false, null,
                com.example.ai_review.diff.AnalysisMode.FAST,
                "FAST",
                false,
                1,
                null,
                new MergeRiskReport(RiskLevel.HIGH, "检测到合并风险",
                        List.of(new MergeRiskItem("依赖/构建", "pom.xml", "HIGH",
                                "依赖发生变更", "执行 mvn test"))),
                new ReviewReport("OK", RiskLevel.LOW,
                        List.of(), List.of(), "deepseek-v4-flash")
        );

        String md = formatter.format(response);

        assertTrue(md.contains("合并风险"));
        assertTrue(md.contains("依赖/构建"));
        assertTrue(md.contains("pom.xml"));
        assertTrue(md.contains("执行 mvn test"));
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

    @Test
    void usesLongerFenceWhenSnippetContainsBackticks() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 1, "PR", "a", "open", "main", "feat",
                1, 1, 0, 1, false, null,
                new ReviewReport("OK", RiskLevel.LOW,
                        List.of(new RiskItem("App.java", "LOW", "t", "r", "s",
                                1, "code with ``` inside", "fix with ``` too")),
                        List.of(), "m")
        );

        String md = formatter.format(response);

        // Should use longer fence (4 or more backticks), not ``` which would break
        assertTrue(md.contains("````"));
        // The content should be intact
        assertTrue(md.contains("code with ``` inside"));
    }

    @Test
    void infersYamlLanguage() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 1, "PR", "a", "open", "main", "feat",
                1, 1, 0, 1, false, null,
                new ReviewReport("OK", RiskLevel.LOW,
                        List.of(new RiskItem("config.yml", "LOW", "t", "r", "s",
                                1, "key: value", null)),
                        List.of(), "m")
        );

        String md = formatter.format(response);

        assertTrue(md.contains("```yaml"));
    }

    @Test
    void infersJavascriptLanguage() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 1, "PR", "a", "open", "main", "feat",
                1, 1, 0, 1, false, null,
                new ReviewReport("OK", RiskLevel.LOW,
                        List.of(new RiskItem("web.js", "LOW", "t", "r", "s",
                                1, "const x = 1;", null)),
                        List.of(), "m")
        );

        String md = formatter.format(response);

        assertTrue(md.contains("```javascript"));
    }

    @Test
    void doesNotOutputBlankSnippet() {
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "o", "r", 1, "PR", "a", "open", "main", "feat",
                1, 1, 0, 1, false, null,
                new ReviewReport("OK", RiskLevel.LOW,
                        List.of(new RiskItem("App.java", "LOW", "t", "r", "s",
                                1, "", "   ")),
                        List.of(), "m")
        );

        String md = formatter.format(response);

        // No code blocks should be output for empty/blank snippets
        assertFalse(md.contains("代码片段"));
        assertFalse(md.contains("示例修复"));
    }
}
