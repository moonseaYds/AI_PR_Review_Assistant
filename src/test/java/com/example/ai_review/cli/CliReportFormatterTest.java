package com.example.ai_review.cli;

import com.example.ai_review.diff.AnalysisMode;
import com.example.ai_review.report.AnalyzePullRequestResponse;
import com.example.ai_review.report.MergeRiskReport;
import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskItem;
import com.example.ai_review.review.RiskLevel;
import com.example.ai_review.review.SuggestionItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CliReportFormatterTest {

    private final CliReportFormatter formatter = new CliReportFormatter();

    @Test
    void shouldRenderMarkdownReportWithEvidence() {
        ReviewReport review = new ReviewReport(
                "发现潜在空指针风险。",
                RiskLevel.HIGH,
                List.of(new RiskItem(
                        "src/main/java/Demo.java",
                        "HIGH",
                        "空指针风险",
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
                        null,
                        null
                )),
                "deepseek-test"
        );
        AnalyzePullRequestResponse response = new AnalyzePullRequestResponse(
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
                false,
                null,
                AnalysisMode.FAST,
                "FAST 模式",
                false,
                1,
                null,
                new MergeRiskReport(RiskLevel.LOW, "未检测到明显风险。", List.of()),
                review
        );

        String markdown = formatter.format(response);

        assertThat(markdown)
                .contains("# AI PR Review Report")
                .contains("Repository: local/demo")
                .contains("Source: Local Diff Review")
                .contains("空指针风险")
                .contains("```java")
                .contains("\"admin\".equals(username)")
                .doesNotContain("deepseek-test");
    }
}
