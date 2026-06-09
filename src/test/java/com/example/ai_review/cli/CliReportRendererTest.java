package com.example.ai_review.cli;

import com.example.ai_review.diff.AnalysisMode;
import com.example.ai_review.report.AnalyzePullRequestResponse;
import com.example.ai_review.report.MergeRiskReport;
import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliReportRendererTest {

    private final CliReportRenderer renderer = new CliReportRenderer(
            new CliReportFormatter(),
            new CliHtmlReportFormatter()
    );

    @Test
    void shouldRenderMarkdownByDefault() {
        String report = renderer.format(response(), null);

        assertThat(report)
                .startsWith("# AI PR Review Report")
                .doesNotContain("<!DOCTYPE html>");
    }

    @Test
    void shouldRenderHtmlWhenConfigured() {
        String report = renderer.format(response(), "html");

        assertThat(report)
                .contains("<!DOCTYPE html>")
                .contains("AI PR Review Report");
    }

    @Test
    void shouldRejectUnsupportedFormat() {
        assertThatThrownBy(() -> renderer.format(response(), "pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AI_PR_REVIEW_OUTPUT_FORMAT");
    }

    private AnalyzePullRequestResponse response() {
        ReviewReport review = new ReviewReport(
                "没有发现明显问题。",
                RiskLevel.LOW,
                List.of(),
                List.of(),
                null
        );
        return new AnalyzePullRequestResponse(
                "local", "demo", 0, "Local Diff Review", "local", "local",
                "main", "working-tree", 0, 0, 0, 0, false, null,
                AnalysisMode.FAST, "FAST 模式", false, 1, null,
                new MergeRiskReport(RiskLevel.LOW, "未检测到明显风险。", List.of()),
                review
        );
    }
}
