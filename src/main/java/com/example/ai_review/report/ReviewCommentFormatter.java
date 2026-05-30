package com.example.ai_review.report;

import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskItem;
import com.example.ai_review.review.SuggestionItem;
import org.springframework.stereotype.Service;

@Service
public class ReviewCommentFormatter {

    public String format(AnalyzePullRequestResponse response) {
        StringBuilder md = new StringBuilder();

        md.append("## AI PR Review Assistant\n\n");

        md.append("### PR 概览\n\n");
        md.append("- 仓库：").append(escape(response.owner())).append("/").append(escape(response.repo())).append("\n");
        md.append("- PR：#").append(response.pullNumber()).append(" ").append(escape(response.title())).append("\n");
        md.append("- 作者：").append(escape(response.author())).append("\n");
        md.append("- 分支：").append(escape(response.headBranch()))
                .append(" → ").append(escape(response.baseBranch())).append("\n");
        md.append("- 变更：").append(response.totalFiles()).append(" 个文件")
                .append("，+").append(response.totalAdditions())
                .append(" / -").append(response.totalDeletions()).append("\n");

        if (response.truncated()) {
            md.append("- ⚠️ Diff 已截断：").append(escape(
                    response.truncationReason() != null ? response.truncationReason() : "内容过长")).append("\n");
        }

        md.append("\n");

        ReviewReport review = response.review();
        if (review != null) {
            md.append("### AI 总结\n\n");
            md.append(escape(review.summary() != null ? review.summary() : "无总结")).append("\n\n");

            md.append("### 风险等级\n\n");
            md.append(escape(review.riskLevel() != null ? review.riskLevel().name() : "UNKNOWN")).append("\n\n");

            if (review.risks() != null && !review.risks().isEmpty()) {
                md.append("### 风险点\n\n");
                int i = 1;
                for (RiskItem risk : review.risks()) {
                    md.append(i).append(". **").append(escape(risk.level()))
                            .append(" - ").append(escape(risk.title())).append("**\n");
                    md.append("   - 文件：").append(escape(risk.file())).append("\n");
                    md.append("   - 原因：").append(escape(risk.reason())).append("\n");
                    if (risk.suggestion() != null && !risk.suggestion().isBlank()) {
                        md.append("   - 建议：").append(escape(risk.suggestion())).append("\n");
                    }
                    md.append("\n");
                    i++;
                }
            }

            if (review.suggestions() != null && !review.suggestions().isEmpty()) {
                md.append("### Review 建议\n\n");
                for (SuggestionItem s : review.suggestions()) {
                    md.append("- [").append(escape(s.category())).append("] ")
                            .append(escape(s.file())).append("：")
                            .append(escape(s.content())).append("\n");
                }
                md.append("\n");
            }
        }

        md.append("> 本评论由 AI PR Review Assistant 自动生成，仅作为辅助审查建议，不替代人工 Code Review。\n");

        return md.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }
}
