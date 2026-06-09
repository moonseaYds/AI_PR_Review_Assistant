package com.example.ai_review.cli;

import com.example.ai_review.report.AnalyzePullRequestResponse;
import com.example.ai_review.report.MergeRiskItem;
import com.example.ai_review.report.MergeRiskReport;
import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskItem;
import com.example.ai_review.review.SuggestionItem;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CliHtmlReportFormatter {

    public String format(AnalyzePullRequestResponse response) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>AI PR Review Report</title>
                    <style>
                """);
        html.append(styles());
        html.append("""
                    </style>
                </head>
                <body>
                    <main class="container">
                        <header class="header">
                            <h1>AI PR Review Report</h1>
                            <p class="header-subtitle">静态 HTML 报告 · AI PR Review Assistant</p>
                        </header>
                        <section class="result-card">
                """);

        appendOverview(html, response);
        appendStatistics(html, response);
        appendReviewSummary(html, response.review());
        appendMergeRisk(html, response.mergeRisk());
        appendRisks(html, response.review());
        appendSuggestions(html, response.review());

        html.append("""
                        </section>
                        <footer class="footer">
                            <p>AI Review 结果仅作辅助审查建议，不替代人工 Code Review 和自动化测试。</p>
                            <p>Generated at %s</p>
                        </footer>
                    </main>
                </body>
                </html>
                """.formatted(escape(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))));
        return html.toString();
    }

    private void appendOverview(StringBuilder html, AnalyzePullRequestResponse response) {
        html.append("""
                <section class="result-section">
                    <div class="section-title">PR 信息</div>
                    <div class="pr-meta">
                """);
        appendMeta(html, "仓库", value(response.owner()) + "/" + value(response.repo()));
        if (response.pullNumber() > 0) {
            appendMeta(html, "Pull Request", "#" + response.pullNumber() + " " + value(response.title()));
        } else {
            appendMeta(html, "来源", value(response.title()));
        }
        appendMeta(html, "作者", value(response.author()));
        appendMeta(html, "状态", value(response.state()));
        appendMeta(html, "源分支", value(response.headBranch()));
        appendMeta(html, "目标分支", value(response.baseBranch()));
        appendMeta(html, "Context 模式", response.analysisMode() != null ? response.analysisMode().name() : "FAST");
        appendMeta(html, "分批 Review", response.batchReview() ? response.reviewBatches() + " 批" : "否");
        html.append("</div>");
        appendStrategy(html, response.contextStrategy());
        appendStrategy(html, response.batchStrategy());
        html.append("</section>");
    }

    private void appendStatistics(StringBuilder html, AnalyzePullRequestResponse response) {
        html.append("""
                <section class="result-section">
                    <div class="section-title">变更统计</div>
                    <div class="stats-row">
                """);
        appendStat(html, response.totalFiles(), "文件", "");
        appendStat(html, response.totalAdditions(), "新增行", " additions");
        appendStat(html, response.totalDeletions(), "删除行", " deletions");
        appendStat(html, response.totalChanges(), "总变更", "");
        html.append("</div>");
        if (response.truncated()) {
            html.append("<div class=\"truncation-badge\">Diff 已截断：")
                    .append(escape(value(response.truncationReason())))
                    .append("</div>");
        }
        html.append("</section>");
    }

    private void appendReviewSummary(StringBuilder html, ReviewReport review) {
        String summary = review != null ? review.summary() : "无总结";
        String riskLevel = review != null && review.riskLevel() != null ? review.riskLevel().name() : "UNKNOWN";
        html.append("""
                <section class="result-section">
                    <div class="section-title">AI 分析总结</div>
                """);
        html.append("<p class=\"review-summary\">").append(escape(value(summary))).append("</p>");
        appendRiskBadge(html, riskLevel);
        if (review != null && review.model() != null && !review.model().isBlank()) {
            html.append("<span class=\"model-info\">模型：").append(escape(review.model())).append("</span>");
        }
        html.append("</section>");
    }

    private void appendMergeRisk(StringBuilder html, MergeRiskReport mergeRisk) {
        if (mergeRisk == null) {
            return;
        }
        html.append("<section class=\"result-section\"><div class=\"section-title\">合并风险（")
                .append(mergeRisk.items() == null ? 0 : mergeRisk.items().size())
                .append("）</div>");
        html.append("<p class=\"review-summary\">").append(escape(value(mergeRisk.summary()))).append("</p>");
        appendRiskBadge(html, mergeRisk.riskLevel() != null ? mergeRisk.riskLevel().name() : "LOW");
        if (mergeRisk.items() == null || mergeRisk.items().isEmpty()) {
            html.append("<div class=\"empty-state\">未检测到明显合并风险</div>");
        } else {
            html.append("<ul class=\"risk-list merge-risk-list\">");
            for (MergeRiskItem item : mergeRisk.items()) {
                html.append("<li class=\"risk-item\"><div class=\"risk-header\">");
                appendRiskBadge(html, value(item.level()));
                html.append("<span class=\"risk-title\">").append(escape(value(item.category()))).append("</span></div>");
                html.append("<div class=\"risk-file\">").append(escape(value(item.file()))).append("</div>");
                html.append("<div class=\"risk-reason\">").append(escape(value(item.reason()))).append("</div>");
                html.append("<div class=\"risk-suggestion\">→ ").append(escape(value(item.suggestion()))).append("</div>");
                html.append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</section>");
    }

    private void appendRisks(StringBuilder html, ReviewReport review) {
        var risks = review != null ? review.risks() : null;
        html.append("<section class=\"result-section\"><div class=\"section-title\">风险点（")
                .append(risks == null ? 0 : risks.size())
                .append("）</div>");
        if (risks == null || risks.isEmpty()) {
            html.append("<div class=\"empty-state\"><div class=\"empty-state-icon\">✓</div><div>未发现明显风险</div></div>");
        } else {
            html.append("<ul class=\"risk-list\">");
            for (RiskItem risk : risks) {
                html.append("<li class=\"risk-item\"><div class=\"risk-header\">");
                appendRiskBadge(html, value(risk.level()));
                html.append("<span class=\"risk-title\">").append(escape(value(risk.title()))).append("</span></div>");
                appendFileLocation(html, risk.file(), risk.lineNumber());
                html.append("<div class=\"risk-reason\">").append(escape(value(risk.reason()))).append("</div>");
                html.append("<div class=\"risk-suggestion\">→ ").append(escape(value(risk.suggestion()))).append("</div>");
                appendCodeBlock(html, risk.codeSnippet());
                appendExampleFix(html, "示例修复", risk.exampleFix());
                html.append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</section>");
    }

    private void appendSuggestions(StringBuilder html, ReviewReport review) {
        var suggestions = review != null ? review.suggestions() : null;
        html.append("<section class=\"result-section\"><div class=\"section-title\">Review 建议（")
                .append(suggestions == null ? 0 : suggestions.size())
                .append("）</div>");
        if (suggestions == null || suggestions.isEmpty()) {
            html.append("<div class=\"empty-state\">暂无建议</div>");
        } else {
            html.append("<ul class=\"suggestion-list\">");
            for (SuggestionItem suggestion : suggestions) {
                html.append("<li class=\"suggestion-item\">");
                html.append("<span class=\"suggestion-category\">").append(escape(value(suggestion.category()))).append("</span>");
                appendFileLocation(html, suggestion.file(), suggestion.lineNumber());
                html.append("<div class=\"suggestion-content\">").append(escape(value(suggestion.content()))).append("</div>");
                appendCodeBlock(html, suggestion.codeSnippet());
                appendExampleFix(html, "示例改进", suggestion.exampleFix());
                html.append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</section>");
    }

    private void appendMeta(StringBuilder html, String label, String value) {
        html.append("<div class=\"pr-meta-item\"><span class=\"pr-meta-label\">")
                .append(escape(label))
                .append("</span><span class=\"pr-meta-value\">")
                .append(escape(value))
                .append("</span></div>");
    }

    private void appendStat(StringBuilder html, int value, String label, String cssClass) {
        html.append("<div class=\"stat-item\"><div class=\"stat-value")
                .append(cssClass)
                .append("\">")
                .append(value)
                .append("</div><div class=\"stat-label\">")
                .append(escape(label))
                .append("</div></div>");
    }

    private void appendStrategy(StringBuilder html, String strategy) {
        if (strategy != null && !strategy.isBlank()) {
            html.append("<div class=\"context-strategy\">").append(escape(strategy)).append("</div>");
        }
    }

    private void appendRiskBadge(StringBuilder html, String level) {
        String normalized = value(level).toUpperCase();
        html.append("<span class=\"risk-level ")
                .append(escape(normalized))
                .append("\">")
                .append(escape(normalized))
                .append("</span>");
    }

    private void appendFileLocation(StringBuilder html, String file, Integer lineNumber) {
        html.append("<div class=\"risk-file\">").append(escape(value(file)));
        if (lineNumber != null) {
            html.append("（第 ").append(lineNumber).append(" 行附近）");
        }
        html.append("</div>");
    }

    private void appendCodeBlock(StringBuilder html, String content) {
        if (content != null && !content.isBlank()) {
            html.append("<pre class=\"code-snippet\"><code>")
                    .append(escape(content))
                    .append("</code></pre>");
        }
    }

    private void appendExampleFix(StringBuilder html, String label, String content) {
        if (content != null && !content.isBlank()) {
            html.append("<div class=\"example-fix-label\">")
                    .append(escape(label))
                    .append("</div>");
            appendCodeBlock(html, content);
        }
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String styles() {
        return """
                :root {
                    --color-bg: #fafbfc;
                    --color-surface: #ffffff;
                    --color-border: #d0d7de;
                    --color-text: #1f2328;
                    --color-text-secondary: #656d76;
                    --color-text-muted: #8b949e;
                    --color-accent: #0969da;
                    --color-success: #1a7f37;
                    --color-warning: #9a6700;
                    --color-danger: #cf222e;
                    --color-bg-success: #dafbe1;
                    --color-bg-warning: #fff8c5;
                    --color-bg-danger: #ffebe9;
                    --color-bg-info: #ddf4ff;
                    --radius: 6px;
                    --font-stack: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans", Helvetica, Arial, sans-serif;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    font-family: var(--font-stack);
                    font-size: 14px;
                    line-height: 1.6;
                    color: var(--color-text);
                    background: var(--color-bg);
                }
                .container { width: min(100% - 40px, 920px); margin: 0 auto; padding: 32px 0; }
                .header { margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid var(--color-border); }
                .header h1 { font-size: 22px; font-weight: 650; }
                .header-subtitle { margin-top: 4px; color: var(--color-text-secondary); }
                .result-card {
                    background: var(--color-surface);
                    border: 1px solid var(--color-border);
                    border-radius: var(--radius);
                    box-shadow: 0 1px 0 rgba(27, 31, 36, 0.04);
                }
                .result-section { padding: 20px; border-bottom: 1px solid var(--color-border); }
                .result-section:last-child { border-bottom: none; }
                .section-title { font-size: 16px; font-weight: 650; margin-bottom: 12px; }
                .pr-meta { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 10px; }
                .pr-meta-item { padding: 10px 12px; background: #f6f8fa; border: 1px solid var(--color-border); border-radius: var(--radius); }
                .pr-meta-label { display: block; font-size: 12px; color: var(--color-text-muted); }
                .pr-meta-value { display: block; margin-top: 2px; font-weight: 600; overflow-wrap: anywhere; }
                .context-strategy { margin-top: 10px; padding: 10px 12px; background: var(--color-bg-info); border-radius: var(--radius); color: var(--color-text-secondary); }
                .stats-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
                .stat-item { padding: 14px 12px; background: #f6f8fa; border: 1px solid var(--color-border); border-radius: var(--radius); text-align: center; }
                .stat-value { font-size: 22px; font-weight: 700; }
                .stat-value.additions { color: var(--color-success); }
                .stat-value.deletions { color: var(--color-danger); }
                .stat-label { color: var(--color-text-secondary); font-size: 12px; }
                .truncation-badge { display: inline-block; margin-top: 10px; padding: 4px 10px; border-radius: 12px; color: var(--color-warning); background: var(--color-bg-warning); }
                .review-summary { color: var(--color-text); line-height: 1.7; overflow-wrap: anywhere; margin-bottom: 10px; }
                .risk-level { display: inline-flex; align-items: center; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; }
                .risk-level.LOW { color: var(--color-success); background: var(--color-bg-success); }
                .risk-level.MEDIUM { color: var(--color-warning); background: var(--color-bg-warning); }
                .risk-level.HIGH { color: var(--color-danger); background: var(--color-bg-danger); }
                .risk-level.UNKNOWN { color: var(--color-text-secondary); background: #f6f8fa; }
                .model-info { display: inline-block; margin-left: 8px; padding: 2px 8px; border-radius: 12px; color: var(--color-accent); background: var(--color-bg-info); font-size: 12px; }
                .risk-list, .suggestion-list { list-style: none; }
                .risk-item, .suggestion-item { padding: 14px 0; border-bottom: 1px solid var(--color-border); }
                .risk-item:last-child, .suggestion-item:last-child { border-bottom: none; padding-bottom: 0; }
                .risk-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
                .risk-title { font-weight: 650; overflow-wrap: anywhere; }
                .risk-file { margin: 5px 0; color: var(--color-text-secondary); font-size: 12px; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; overflow-wrap: anywhere; }
                .risk-reason, .risk-suggestion, .suggestion-content { color: var(--color-text-secondary); margin-top: 4px; overflow-wrap: anywhere; }
                .risk-suggestion { color: var(--color-success); }
                .suggestion-category { display: inline-block; margin-right: 8px; padding: 1px 6px; border-radius: 8px; color: var(--color-accent); background: var(--color-bg-info); font-size: 11px; font-weight: 600; }
                .empty-state { padding: 24px 16px; color: var(--color-text-secondary); text-align: center; }
                .empty-state-icon { font-size: 26px; margin-bottom: 6px; }
                .code-snippet { margin-top: 8px; padding: 10px 12px; background: #f6f8fa; border: 1px solid var(--color-border); border-radius: var(--radius); overflow-x: auto; white-space: pre-wrap; word-break: break-word; }
                .code-snippet code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 12px; line-height: 1.5; }
                .example-fix-label { margin-top: 10px; color: var(--color-success); font-size: 12px; font-weight: 700; }
                .footer { color: var(--color-text-muted); font-size: 12px; text-align: center; padding: 18px 0; }
                @media (max-width: 640px) {
                    .container { width: min(100% - 24px, 920px); padding: 20px 0; }
                    .stats-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
                    .result-section { padding: 16px; }
                }
                """;
    }
}
