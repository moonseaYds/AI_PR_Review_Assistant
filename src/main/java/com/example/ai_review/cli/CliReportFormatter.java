package com.example.ai_review.cli;

import com.example.ai_review.report.AnalyzePullRequestResponse;
import com.example.ai_review.report.MergeRiskItem;
import com.example.ai_review.report.MergeRiskReport;
import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskItem;
import com.example.ai_review.review.SuggestionItem;
import org.springframework.stereotype.Component;

@Component
public class CliReportFormatter {

    public String format(AnalyzePullRequestResponse response) {
        StringBuilder md = new StringBuilder();
        md.append("# AI PR Review Report\n\n");

        appendOverview(md, response);
        appendMergeRisk(md, response.mergeRisk());
        appendReview(md, response.review());

        md.append("\n> AI Review 结果仅作辅助审查建议，不替代人工 Code Review 和自动化测试。\n");
        return md.toString();
    }

    private void appendOverview(StringBuilder md, AnalyzePullRequestResponse response) {
        md.append("## Overview\n\n");
        md.append("- Repository: ").append(value(response.owner())).append("/")
                .append(value(response.repo())).append("\n");
        if (response.pullNumber() > 0) {
            md.append("- Pull Request: #").append(response.pullNumber())
                    .append(" ").append(value(response.title())).append("\n");
        } else {
            md.append("- Source: ").append(value(response.title())).append("\n");
        }
        md.append("- Branches: ").append(value(response.headBranch()))
                .append(" -> ").append(value(response.baseBranch())).append("\n");
        md.append("- Analysis Mode: ").append(response.analysisMode()).append("\n");
        md.append("- Changed Files: ").append(response.totalFiles())
                .append(", +").append(response.totalAdditions())
                .append(" / -").append(response.totalDeletions())
                .append(", total ").append(response.totalChanges()).append("\n");
        if (response.truncated()) {
            md.append("- Diff Truncated: ").append(value(response.truncationReason())).append("\n");
        }
        if (response.batchReview()) {
            md.append("- Batch Review: ").append(response.reviewBatches()).append(" batches\n");
        }
        md.append("\n");
    }

    private void appendMergeRisk(StringBuilder md, MergeRiskReport mergeRisk) {
        if (mergeRisk == null) {
            return;
        }

        md.append("## Merge Risk\n\n");
        md.append("- Level: ").append(mergeRisk.riskLevel()).append("\n");
        md.append("- Summary: ").append(value(mergeRisk.summary())).append("\n\n");

        if (mergeRisk.items() != null && !mergeRisk.items().isEmpty()) {
            int index = 1;
            for (MergeRiskItem item : mergeRisk.items()) {
                md.append(index++).append(". **").append(value(item.level()))
                        .append(" - ").append(value(item.category())).append("**\n");
                md.append("   - File: ").append(value(item.file())).append("\n");
                md.append("   - Reason: ").append(value(item.reason())).append("\n");
                md.append("   - Suggestion: ").append(value(item.suggestion())).append("\n\n");
            }
        }
    }

    private void appendReview(StringBuilder md, ReviewReport review) {
        if (review == null) {
            md.append("## AI Review\n\nNo review result.\n");
            return;
        }

        md.append("## AI Summary\n\n");
        md.append(value(review.summary())).append("\n\n");
        md.append("## Risk Level\n\n");
        md.append(review.riskLevel() != null ? review.riskLevel() : "UNKNOWN").append("\n\n");

        appendRisks(md, review);
        appendSuggestions(md, review);
    }

    private void appendRisks(StringBuilder md, ReviewReport review) {
        if (review.risks() == null || review.risks().isEmpty()) {
            return;
        }

        md.append("## Risks\n\n");
        int index = 1;
        for (RiskItem risk : review.risks()) {
            md.append(index++).append(". **").append(value(risk.level()))
                    .append(" - ").append(value(risk.title())).append("**\n");
            appendLocation(md, risk.file(), risk.lineNumber());
            md.append("   - Reason: ").append(value(risk.reason())).append("\n");
            md.append("   - Suggestion: ").append(value(risk.suggestion())).append("\n");
            appendCodeBlock(md, "   - Code", risk.file(), risk.codeSnippet());
            appendCodeBlock(md, "   - Example Fix", risk.file(), risk.exampleFix());
            md.append("\n");
        }
    }

    private void appendSuggestions(StringBuilder md, ReviewReport review) {
        if (review.suggestions() == null || review.suggestions().isEmpty()) {
            return;
        }

        md.append("## Suggestions\n\n");
        for (SuggestionItem suggestion : review.suggestions()) {
            md.append("- **").append(value(suggestion.category())).append("** ");
            md.append(value(suggestion.content())).append("\n");
            appendLocation(md, suggestion.file(), suggestion.lineNumber());
            appendCodeBlock(md, "  Code", suggestion.file(), suggestion.codeSnippet());
            appendCodeBlock(md, "  Example Fix", suggestion.file(), suggestion.exampleFix());
        }
        md.append("\n");
    }

    private void appendLocation(StringBuilder md, String file, Integer lineNumber) {
        md.append("   - File: ").append(value(file));
        if (lineNumber != null) {
            md.append(":").append(lineNumber);
        }
        md.append("\n");
    }

    private void appendCodeBlock(StringBuilder md, String label, String file, String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        md.append(label).append(":\n");
        md.append("```").append(language(file)).append("\n");
        md.append(content).append("\n");
        md.append("```\n");
    }

    private String language(String file) {
        if (file == null) {
            return "";
        }
        String lower = file.toLowerCase();
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return "typescript";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".properties")) return "properties";
        if (lower.endsWith(".sh")) return "bash";
        return "";
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
