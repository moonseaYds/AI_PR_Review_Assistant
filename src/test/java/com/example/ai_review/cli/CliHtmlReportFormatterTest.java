package com.example.ai_review.cli;

import com.example.ai_review.diff.AnalysisMode;
import com.example.ai_review.report.AnalyzePullRequestResponse;
import com.example.ai_review.report.MergeRiskItem;
import com.example.ai_review.report.MergeRiskReport;
import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskItem;
import com.example.ai_review.review.RiskLevel;
import com.example.ai_review.review.SuggestionItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CliHtmlReportFormatterTest {

    private final CliHtmlReportFormatter formatter = new CliHtmlReportFormatter();

    @Test
    void shouldRenderStandaloneHtmlReportWithEscapedEvidence() {
        String html = formatter.format(responseWithEvidence());

        assertThat(html)
                .contains("<!DOCTYPE html>")
                .contains("<style>")
                .contains("AI PR Review Report")
                .contains("local/demo")
                .contains("合并风险（1）")
                .contains("风险点（1）")
                .contains("Review 建议（1）")
                .contains("risk-level HIGH")
                .contains("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;")
                .contains("&quot;admin&quot;.equals(username)")
                .contains("示例修复")
                .doesNotContain("<script>alert");
    }

    @Test
    void shouldRenderEmptyStatesWhenNoRisksOrSuggestions() {
        ReviewReport review = new ReviewReport(
                "没有发现明显问题。",
                RiskLevel.LOW,
                List.of(),
                List.of(),
                "deepseek-test"
        );
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
                "local", "empty", 0, "Local Diff Review", "local", "local",
                "main", "working-tree", 0, 0, 0, 0, false, null,
                AnalysisMode.FAST, "FAST 模式", false, 1, null,
                new MergeRiskReport(RiskLevel.LOW, "未检测到明显风险。", List.of()),
                review
        );

        String html = formatter.format(response);

        assertThat(html)
                .contains("未发现明显风险")
                .contains("暂无建议")
                .contains("未检测到明显合并风险");
    }

    private AnalyzePullRequestResponse responseWithEvidence() {
        ReviewReport review = new ReviewReport(
                "发现潜在空指针风险。",
                RiskLevel.HIGH,
                List.of(new RiskItem(
                        "src/main/java/Demo.java",
                        "HIGH",
                        "<script>alert(\"x\")</script>",
                        "username 可能为空。",
                        "先判空再比较。",
                        12,
                        "username.equals(\"admin\")",
                        "\"admin\".equals(username)"
                )),
                List.of(new SuggestionItem(
                        "src/main/java/Demo.java",
                        "correctness",
                        "补充参数校验。",
                        12,
                        "if (username == null) { return; }",
                        "Objects.requireNonNull(username)"
                )),
                "deepseek-test"
        );
        return new AnalyzePullRequestResponse(
                "local",
                "demo",
                0,
                "Local Diff Review",
                "local",
                "local",
                "main",
                "working-tree",
                1,
                1,
                0,
                1,
                true,
                "patch 超过限制",
                AnalysisMode.FAST,
                "FAST 模式",
                false,
                1,
                null,
                new MergeRiskReport(RiskLevel.HIGH, "检测到合并风险。", List.of(
                        new MergeRiskItem("HIGH", "安全/权限", "README.md",
                                "涉及 Token 文案。", "确认没有提交真实密钥。")
                )),
                review
        );
    }
}
