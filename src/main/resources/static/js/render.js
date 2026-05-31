/**
 * Render layer — pure DOM construction.
 * All functions accept a container element and data.
 */
const Render = (() => {

    function clear(container) {
        container.innerHTML = "";
    }

    function loading(container) {
        container.innerHTML = (
            '<div class="loading-state">' +
                '<div class="spinner"></div>' +
                '<span>正在分析 PR，请稍候&hellip;</span>' +
            '</div>'
        );
    }

    function error(container, err) {
        var message = typeof err === "string" ? err : (err.message || "未知错误");
        var code = (typeof err === "object" && err.code) ? err.code : "";
        var suggestion = (typeof err === "object" && err.suggestion) ? err.suggestion : "";

        var html = '<div class="error-state">';
        html += '<div class="error-title">分析失败</div>';
        if (code) {
            html += '<div class="error-code">错误码：' + escapeHTML(code) + '</div>';
        }
        html += '<div class="error-message">' + escapeHTML(message) + '</div>';
        if (suggestion) {
            html += '<div class="error-suggestion">建议：' + escapeHTML(suggestion) + '</div>';
        }
        if (typeof err === "object" && "retryable" in err) {
            html += '<div class="error-retryable">可重试：' + (err.retryable ? "是" : "否") + '</div>';
        }
        html += '</div>';
        container.innerHTML = html;
    }

    function result(container, data) {
        const review = data.review || {};
        const riskLevel = review.riskLevel || "UNKNOWN";
        const risks = Array.isArray(review.risks) ? review.risks : [];
        const suggestions = Array.isArray(review.suggestions) ? review.suggestions : [];

        let html = '<div class="result-card">';

        // PR metadata
        html += (
            '<div class="result-section">' +
                '<div class="section-title">PR 信息</div>' +
                '<div class="pr-meta">' +
                    metaItem("仓库", data.owner + "/" + data.repo) +
                    metaItem("标题", data.title) +
                    metaItem("作者", data.author) +
                    metaItem("状态", data.state) +
                    metaItem("源分支", data.headBranch) +
                    metaItem("目标分支", data.baseBranch) +
                    metaItem("Context 模式", data.analysisMode || "FAST") +
                    metaItem("分批 Review", data.batchReview ? (data.reviewBatches || 1) + " 批" : "否") +
                '</div>' +
                (data.contextStrategy ? (
                    '<div class="context-strategy">' + escapeHTML(data.contextStrategy) + '</div>'
                ) : "") +
                (data.batchStrategy ? (
                    '<div class="context-strategy">' + escapeHTML(data.batchStrategy) + '</div>'
                ) : "") +
            '</div>'
        );

        // Statistics
        html += (
            '<div class="result-section">' +
                '<div class="section-title">变更统计</div>' +
                '<div class="stats-row">' +
                    statItem(data.totalFiles, "文件") +
                    statItem(data.totalAdditions, "新增行", "additions") +
                    statItem(data.totalDeletions, "删除行", "deletions") +
                    statItem(data.totalChanges, "总变更") +
                '</div>' +
                (data.truncated ? (
                    '<div class="truncation-badge">' +
                        '&#9888; Diff 已截断：' + escapeHTML(data.truncationReason || "内容过长") +
                    '</div>'
                ) : "") +
            '</div>'
        );

        // AI Summary
        html += (
            '<div class="result-section">' +
                '<div class="section-title">AI 分析总结</div>' +
                '<p class="review-summary">' + escapeHTML(review.summary || "无总结") + '</p>' +
                '<span class="risk-level ' + escapeHTML(riskLevel) + '">' +
                    escapeHTML(riskLevel) +
                '</span>' +
                (review.model ? (
                    '<span class="model-info">模型：' + escapeHTML(review.model) + '</span>'
                ) : "") +
            '</div>'
        );

        // Risks
        html += '<div class="result-section">';
        html += '<div class="section-title">风险点（' + risks.length + '）</div>';
        if (risks.length === 0) {
            html += (
                '<div class="empty-state">' +
                    '<div class="empty-state-icon">&#10003;</div>' +
                    '<div>未发现明显风险</div>' +
                '</div>'
            );
        } else {
            html += '<ul class="risk-list">';
            for (const r of risks) {
                var loc = r.lineNumber ? "（第 " + r.lineNumber + " 行附近）" : "";
                html += (
                    '<li class="risk-item">' +
                        '<div class="risk-header">' +
                            '<span class="risk-level ' + escapeHTML(r.level || "LOW") + '">' +
                                escapeHTML(r.level || "LOW") +
                            '</span>' +
                            '<span class="risk-title">' + escapeHTML(r.title || "") + '</span>' +
                        '</div>' +
                        '<div class="risk-file">' + escapeHTML(r.file || "") + escapeHTML(loc) + '</div>' +
                        '<div class="risk-reason">' + escapeHTML(r.reason || "") + '</div>' +
                        '<div class="risk-suggestion">&rarr; ' + escapeHTML(r.suggestion || "") + '</div>' +
                        (r.codeSnippet ? '<pre class="code-snippet"><code>' + escapeHTML(r.codeSnippet) + '</code></pre>' : '') +
                        (r.exampleFix ? '<div class="example-fix-label">示例修复</div><pre class="code-snippet"><code>' + escapeHTML(r.exampleFix) + '</code></pre>' : '') +
                    '</li>'
                );
            }
            html += '</ul>';
        }
        html += '</div>';

        // Suggestions
        html += '<div class="result-section">';
        html += '<div class="section-title">Review 建议（' + suggestions.length + '）</div>';
        if (suggestions.length === 0) {
            html += (
                '<div class="empty-state">' +
                    '<div>暂无建议</div>' +
                '</div>'
            );
        } else {
            html += '<ul class="suggestion-list">';
            for (const s of suggestions) {
                var loc = s.lineNumber ? "（第 " + s.lineNumber + " 行附近）" : "";
                html += (
                    '<li class="suggestion-item">' +
                        '<span class="suggestion-category">' +
                            escapeHTML(s.category || "其他") +
                        '</span>' +
                        '<span class="risk-file">' + escapeHTML(s.file || "") + escapeHTML(loc) + '</span>' +
                        '<div class="suggestion-content">' +
                            escapeHTML(s.content || "") +
                        '</div>' +
                        (s.codeSnippet ? '<pre class="code-snippet"><code>' + escapeHTML(s.codeSnippet) + '</code></pre>' : '') +
                        (s.exampleFix ? '<div class="example-fix-label">示例改进</div><pre class="code-snippet"><code>' + escapeHTML(s.exampleFix) + '</code></pre>' : '') +
                    '</li>'
                );
            }
            html += '</ul>';
        }
        html += '</div>';

        html += '</div>'; // .result-card
        container.innerHTML = html;
    }

    function metaItem(label, value) {
        return (
            '<div class="pr-meta-item">' +
                '<span class="pr-meta-label">' + escapeHTML(label) + '</span>' +
                '<span class="pr-meta-value">' + escapeHTML(value != null ? String(value) : "-") + '</span>' +
            '</div>'
        );
    }

    function statItem(value, label, cssClass) {
        return (
            '<div class="stat-item">' +
                '<div class="stat-value' + (cssClass ? " " + cssClass : "") + '">' +
                    (value != null ? value : 0) +
                '</div>' +
                '<div class="stat-label">' + escapeHTML(label) + '</div>' +
            '</div>'
        );
    }

    function escapeHTML(str) {
        if (str == null) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function publishForm(container, prUrl, data) {
        let html = '<div class="result-section publish-section">';
        html += '<div class="section-title">发布到 GitHub PR</div>';
        html += '<button class="btn-primary" id="publish-btn">发布到 PR 评论</button>';
        html += '<div id="publish-status"></div>';
        html += '</div>';
        container.insertAdjacentHTML("beforeend", html);
    }

    function publishLoading() {
        const btn = document.getElementById("publish-btn");
        if (btn) {
            btn.disabled = true;
            btn.textContent = "发布中...";
        }
    }

    function publishSuccess(commentUrl) {
        const status = document.getElementById("publish-status");
        if (status) {
            status.innerHTML = (
                '<div class="publish-success">' +
                    '已发布到 GitHub PR 评论：' +
                    '<a href="' + escapeHTML(commentUrl) + '" target="_blank" rel="noopener">' +
                        escapeHTML(commentUrl) +
                    '</a>' +
                '</div>'
            );
        }
        const btn = document.getElementById("publish-btn");
        if (btn) {
            btn.disabled = true;
            btn.textContent = "已发布";
        }
    }

    function publishError(err) {
        var message = typeof err === "string" ? err : (err.message || "未知错误");
        var code = (typeof err === "object" && err.code) ? err.code : "";
        var suggestion = (typeof err === "object" && err.suggestion) ? err.suggestion : "";

        var html = '<div class="publish-error">';
        html += '<div class="error-title">发布失败</div>';
        if (code) {
            html += '<div class="error-code">错误码：' + escapeHTML(code) + '</div>';
        }
        html += '<div>' + escapeHTML(message) + '</div>';
        if (suggestion) {
            html += '<div class="error-suggestion">建议：' + escapeHTML(suggestion) + '</div>';
        }
        if (typeof err === "object" && "retryable" in err) {
            html += '<div class="error-retryable">可重试：' + (err.retryable ? "是" : "否") + '</div>';
        }
        html += '</div>';

        var status = document.getElementById("publish-status");
        if (status) {
            status.innerHTML = html;
        }
        var btn = document.getElementById("publish-btn");
        if (btn) {
            btn.disabled = false;
            btn.textContent = "发布到 PR 评论";
        }
    }

    return { clear, loading, error, result, publishForm, publishLoading,
             publishSuccess, publishError };
})();
