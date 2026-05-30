package com.example.ai_review.review;

import com.example.ai_review.diff.DiffReviewContext;
import com.example.ai_review.diff.FileContext;
import org.springframework.stereotype.Service;

@Service
public class ReviewPromptBuilder {

    public String buildSystemPrompt() {
        return """
                你是一位资深代码审查专家，负责对 GitHub Pull Request 进行安全、专业的 Code Review。

                ## 审查原则
                1. 只输出有明确证据支持的问题，不要凭空猜测。
                2. 不确定的问题应放入 "suggestions" 而非 "risks"。
                3. 如果文件变更简单、代码质量良好，可以给出 LOW 风险和正面评价。

                ## 审查维度（重点关注）
                - 正确性：逻辑错误、边界条件、空指针风险
                - 安全性：敏感信息泄露、注入风险、权限校验
                - 异常处理：异常吞没、资源未释放、错误信息不清晰
                - 接口兼容性：公开 API 签名变更、返回值类型修改
                - 性能：不必要的对象创建、N+1 查询、阻塞调用
                - 可维护性：命名不清、魔法数字、过度耦合

                ## 输出格式要求
                必须严格返回以下 JSON 格式，不要包含 markdown 代码块标记，不要包含额外说明文字：

                {
                  "summary": "PR 变更总结（中文，2-5句话）",
                  "riskLevel": "LOW|MEDIUM|HIGH",
                  "risks": [
                    {
                      "file": "文件路径",
                      "level": "LOW|MEDIUM|HIGH",
                      "title": "风险标题（简短）",
                      "reason": "风险原因（具体，引用代码位置）",
                      "suggestion": "修复建议（可执行的代码级建议）",
                      "lineNumber": 42,
                      "codeSnippet": "来自 diff 的相关代码片段",
                      "exampleFix": "示例修复代码或伪代码"
                    }
                  ],
                  "suggestions": [
                    {
                      "file": "文件路径",
                      "category": "类别（性能/可维护性/安全/其他）",
                      "content": "建议内容",
                      "lineNumber": 42,
                      "codeSnippet": "来自 diff 的相关代码片段",
                      "exampleFix": "示例改进方式"
                    }
                  ]
                }

                ## 新增字段说明
                - lineNumber：问题所在的近似行号（整数），无法确定时设为 null。
                - codeSnippet：只能引用 diff 中出现过的代码片段，如果无法确定，返回空字符串，不要编造。
                - exampleFix：示例修复代码或伪代码，用于辅助理解，不要求完全可编译。无法给出时返回空字符串。
                - 不确定的问题放入 suggestions，不要升格成 risks。

                ## 风险等级判定
                - HIGH：可能导致线上故障、安全漏洞或数据丢失
                - MEDIUM：可能导致功能异常或影响其他模块
                - LOW：代码风格、命名等不影响运行的问题

                ## 注意
                - 如果未发现明显风险，risks 数组应为空，但 summary 中应说明"未发现明显风险"。
                - suggestions 可以为空数组。
                - 所有文字使用中文输出。
                - codeSnippet 和 exampleFix 必须来自或基于 diff，不要凭空编造代码。
                """;
    }

    public String buildUserPrompt(DiffReviewContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("请审查以下 Pull Request 变更：\n\n");

        sb.append("## PR 信息\n");
        sb.append("- 仓库：").append(context.owner()).append("/").append(context.repo()).append("\n");
        sb.append("- PR 编号：#").append(context.pullNumber()).append("\n");
        sb.append("- 标题：").append(context.title()).append("\n");
        sb.append("- 文件数：").append(context.totalFiles()).append("\n");
        sb.append("- 新增行数：").append(context.totalAdditions()).append("\n");
        sb.append("- 删除行数：").append(context.totalDeletions()).append("\n");
        sb.append("- 总变更行数：").append(context.totalChanges()).append("\n");

        if (context.truncated()) {
            sb.append("- ⚠️ 注意：diff 内容已被截断，截断原因：")
                    .append(context.truncationReason()).append("\n");
        }

        sb.append("\n## 变更文件\n\n");
        for (int i = 0; i < context.fileContexts().size(); i++) {
            FileContext fc = context.fileContexts().get(i);
            sb.append("### 文件 ").append(i + 1).append("：").append(fc.filename()).append("\n");
            sb.append("- 状态：").append(fc.status()).append("\n");
            sb.append("- 新增 ").append(fc.additions()).append(" 行，删除 ")
                    .append(fc.deletions()).append(" 行\n");

            if (fc.patchTruncated()) {
                sb.append("- ⚠️ 此文件 patch 已被截断\n");
            }

            sb.append("\n```diff\n");
            sb.append(fc.patchExcerpt());
            sb.append("\n```\n\n");
        }

        sb.append("请基于以上 diff 内容，按照系统提示要求的 JSON 格式输出 Review 报告。");
        return sb.toString();
    }
}
