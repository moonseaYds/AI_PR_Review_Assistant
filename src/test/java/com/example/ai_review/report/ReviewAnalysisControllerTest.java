package com.example.ai_review.report;

import com.example.ai_review.common.ErrorCode;
import com.example.ai_review.common.GitHubApiException;
import com.example.ai_review.diff.AnalysisMode;
import com.example.ai_review.github.GitHubPrCommentPublisher;
import com.example.ai_review.github.GitHubPullRequestRef;
import com.example.ai_review.github.GitHubPullRequestUrlParser;
import com.example.ai_review.github.PublishPullRequestCommentResponse;
import com.example.ai_review.review.DeepSeekApiException;
import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewAnalysisService analysisService;

    @MockitoBean
    private GitHubPullRequestUrlParser parser;

    @MockitoBean
    private GitHubPrCommentPublisher commentPublisher;

    @MockitoBean
    private ReviewCommentFormatter commentFormatter;

    @Test
    void analyzeReturnsStructuredResponse() throws Exception {
        when(analysisService.analyze(any(), any())).thenReturn(new AnalyzePullRequestResponse(
                "owner", "repo", 1, "Fix bug", "octocat", "open",
                "main", "feature/fix", 1, 5, 2, 7, false, null,
                new ReviewReport("代码质量良好", RiskLevel.LOW,
                        List.of(), List.of(), "deepseek-v4-flash")
        ));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/owner/repo/pull/1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("owner"))
                .andExpect(jsonPath("$.repo").value("repo"))
                .andExpect(jsonPath("$.pullNumber").value(1))
                .andExpect(jsonPath("$.title").value("Fix bug"))
                .andExpect(jsonPath("$.author").value("octocat"))
                .andExpect(jsonPath("$.state").value("open"))
                .andExpect(jsonPath("$.baseBranch").value("main"))
                .andExpect(jsonPath("$.headBranch").value("feature/fix"))
                .andExpect(jsonPath("$.totalFiles").value(1))
                .andExpect(jsonPath("$.totalAdditions").value(5))
                .andExpect(jsonPath("$.totalDeletions").value(2))
                .andExpect(jsonPath("$.totalChanges").value(7))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.truncationReason").doesNotExist())
                .andExpect(jsonPath("$.analysisMode").value("FAST"))
                .andExpect(jsonPath("$.contextStrategy", containsString("FAST")))
                .andExpect(jsonPath("$.review.summary").value("代码质量良好"))
                .andExpect(jsonPath("$.review.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.review.risks").isEmpty());
    }

    @Test
    void analyzeAcceptsDeepAnalysisMode() throws Exception {
        when(analysisService.analyze(any(), eq(AnalysisMode.DEEP))).thenReturn(new AnalyzePullRequestResponse(
                "owner", "repo", 1, "Deep review", "octocat", "open",
                "main", "feature/deep", 1, 20, 3, 23, false, null,
                AnalysisMode.DEEP, "DEEP 模式：使用更宽上下文预算模拟分批分析覆盖面，单文件限制 8000 字符，总限制 48000 字符",
                new ReviewReport("深度分析完成", RiskLevel.LOW,
                        List.of(), List.of(), "deepseek-v4-flash")
        ));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/owner/repo/pull/1",
                                  "analysisMode": "DEEP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisMode").value("DEEP"))
                .andExpect(jsonPath("$.contextStrategy", containsString("8000")))
                .andExpect(jsonPath("$.contextStrategy", containsString("48000")))
                .andExpect(jsonPath("$.review.summary").value("深度分析完成"));

        verify(analysisService).analyze(any(), eq(AnalysisMode.DEEP));
    }


    @Test
    void analyzeReturns400ForIllegalArgument() throws Exception {
        when(analysisService.analyze(any(), any()))
                .thenThrow(new IllegalArgumentException("当前仅支持 github.com 的 PR 链接"));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://example.com/owner/repo/pull/1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("github.com")));
    }

    @Test
    void analyzeReturns400ForEmptyUrl() throws Exception {
        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void analyzeReturnsInvalidPrUrlForNonGithubDomain() throws Exception {
        when(analysisService.analyze(any(), any()))
                .thenThrow(new com.example.ai_review.common.BadRequestException(
                        com.example.ai_review.common.ErrorCode.INVALID_PR_URL,
                        "当前仅支持 github.com 的 PR 链接",
                        "请输入标准 GitHub PR 链接"));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://example.com/owner/repo/pull/1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PR_URL"))
                .andExpect(jsonPath("$.suggestion").isNotEmpty());
    }

    @Test
    void analyzeReturns502ForGitHubError() throws Exception {
        when(analysisService.analyze(any(), any()))
                .thenThrow(new GitHubApiException("GitHub PR 不存在"));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/owner/repo/pull/99999"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("GITHUB_UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message", containsString("GitHub")));
    }

    @Test
    void analyzeReturns502ForDeepSeekError() throws Exception {
        when(analysisService.analyze(any(), any()))
                .thenThrow(new DeepSeekApiException(
                        "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY"));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/owner/repo/pull/1"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("DEEPSEEK_UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message", containsString("DEEPSEEK_API_KEY")));
    }

    @Test
    void analyzeReturns400ForEmptyChangedFiles() throws Exception {
        when(analysisService.analyze(any(), any()))
                .thenThrow(new IllegalArgumentException("该 PR 没有变更文件"));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/owner/repo/pull/1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("变更文件")));
    }

    @Test
    void analyzeReturnsFullReportWithRisks() throws Exception {
        when(analysisService.analyze(any(), any())).thenReturn(new AnalyzePullRequestResponse(
                "o", "r", 2, "Risky PR", "dev", "open",
                "main", "feat/risky", 2, 20, 5, 25, true, "超过总长度限制",
                new ReviewReport("存在高风险", RiskLevel.HIGH,
                        List.of(new com.example.ai_review.review.RiskItem(
                                "Service.java", "HIGH", "空指针",
                                "未检查 null", "加 if 判断", null, null, null)),
                        List.of(new com.example.ai_review.review.SuggestionItem(
                                "Service.java", "安全", "建议加权限校验", null, null, null)),
                        "deepseek-v4-flash")
        ));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/o/r/pull/2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truncated").value(true))
                .andExpect(jsonPath("$.truncationReason").value("超过总长度限制"))
                .andExpect(jsonPath("$.review.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.review.risks[0].file").value("Service.java"))
                .andExpect(jsonPath("$.review.risks[0].title").value("空指针"))
                .andExpect(jsonPath("$.review.suggestions[0].category").value("安全"));
    }

    // --- publish-comment tests ---

    @Test
    void publishCommentReturnsCommentUrl() throws Exception {
        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 1,
                "https://github.com/o/r/pull/1");
        when(parser.parse("https://github.com/o/r/pull/1")).thenReturn(ref);
        when(commentFormatter.format(any())).thenReturn("## Review\n\nmarkdown");
        when(commentPublisher.publish(ref, "## Review\n\nmarkdown"))
                .thenReturn(new PublishPullRequestCommentResponse(
                        "o", "r", 1,
                        "https://github.com/o/r/pull/1#issuecomment-123"));

        mockMvc.perform(post("/api/reviews/publish-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/o/r/pull/1",
                                  "analysis": {
                                    "owner": "o",
                                    "repo": "r",
                                    "pullNumber": 1,
                                    "title": "PR",
                                    "author": "dev",
                                    "state": "open",
                                    "baseBranch": "main",
                                    "headBranch": "feat",
                                    "totalFiles": 1,
                                    "totalAdditions": 1,
                                    "totalDeletions": 0,
                                    "totalChanges": 1,
                                    "truncated": false,
                                    "truncationReason": null,
                                    "review": {
                                      "summary": "OK",
                                      "riskLevel": "LOW",
                                      "risks": [],
                                      "suggestions": [],
                                      "model": "deepseek-v4-flash"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("o"))
                .andExpect(jsonPath("$.repo").value("r"))
                .andExpect(jsonPath("$.pullNumber").value(1))
                .andExpect(jsonPath("$.commentUrl").value(
                        "https://github.com/o/r/pull/1#issuecomment-123"));
    }

    @Test
    void publishCommentReturns400ForMissingAnalysis() throws Exception {
        mockMvc.perform(post("/api/reviews/publish-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/o/r/pull/1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void publishCommentReturns400ForInvalidUrl() throws Exception {
        when(parser.parse("invalid"))
                .thenThrow(new IllegalArgumentException("当前仅支持 github.com"));

        mockMvc.perform(post("/api/reviews/publish-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "invalid",
                                  "analysis": {
                                    "owner": "o", "repo": "r", "pullNumber": 1,
                                    "title": "T", "author": "a", "state": "open",
                                    "baseBranch": "main", "headBranch": "feat",
                                    "totalFiles": 1, "totalAdditions": 1,
                                    "totalDeletions": 0, "totalChanges": 1,
                                    "truncated": false, "truncationReason": null,
                                    "review": {"summary": "OK", "riskLevel": "LOW",
                                      "risks": [], "suggestions": [], "model": "m"}
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void publishCommentReturns502ForMissingToken() throws Exception {
        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 1,
                "https://github.com/o/r/pull/1");
        when(parser.parse("https://github.com/o/r/pull/1")).thenReturn(ref);
        when(commentFormatter.format(any())).thenReturn("## md");
        when(commentPublisher.publish(ref, "## md"))
                .thenThrow(new GitHubApiException("发布 PR 评论需要配置 GITHUB_TOKEN"));

        mockMvc.perform(post("/api/reviews/publish-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/o/r/pull/1",
                                  "analysis": {
                                    "owner": "o", "repo": "r", "pullNumber": 1,
                                    "title": "T", "author": "a", "state": "open",
                                    "baseBranch": "main", "headBranch": "feat",
                                    "totalFiles": 1, "totalAdditions": 1,
                                    "totalDeletions": 0, "totalChanges": 1,
                                    "truncated": false, "truncationReason": null,
                                    "review": {"summary": "OK", "riskLevel": "LOW",
                                      "risks": [], "suggestions": [], "model": "m"}
                                  }
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("GITHUB_UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message").value("发布 PR 评论需要配置 GITHUB_TOKEN"));
    }

    // --- analyze-diff tests ---

    @Test
    void analyzeDiffReturnsStructuredResponse() throws Exception {
        when(analysisService.analyzeDiff(any())).thenReturn(new AnalyzePullRequestResponse(
                "local", "my-project", 0, "Local Diff Review", "local", "local",
                "main", "working-tree", 1, 1, 1, 2, false, null,
                new ReviewReport("OK", RiskLevel.LOW, List.of(), List.of(), "deepseek-v4-flash")
        ));

        mockMvc.perform(post("/api/reviews/analyze-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repository": "my-project",
                                  "baseBranch": "main",
                                  "headBranch": "working-tree",
                                  "diffText": "diff --git a/A.java b/A.java\\n--- a/A.java\\n+++ b/A.java\\n@@ -1 +1 @@\\n-old\\n+new"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("local"))
                .andExpect(jsonPath("$.repo").value("my-project"))
                .andExpect(jsonPath("$.pullNumber").value(0))
                .andExpect(jsonPath("$.review.riskLevel").value("LOW"));
    }

    @Test
    void analyzeDiffReturns400ForEmptyDiffText() throws Exception {
        mockMvc.perform(post("/api/reviews/analyze-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diffText": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void analyzeDiffReturns400ForInvalidDiff() throws Exception {
        when(analysisService.analyzeDiff(any()))
                .thenThrow(new com.example.ai_review.common.BadRequestException(
                        com.example.ai_review.common.ErrorCode.INVALID_DIFF_TEXT,
                        "无法解析 diff 内容",
                        "请粘贴 git diff 完整输出"));

        mockMvc.perform(post("/api/reviews/analyze-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diffText": "not a valid diff"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DIFF_TEXT"))
                .andExpect(jsonPath("$.suggestion").isNotEmpty());
    }

    @Test
    void analyzeReturnsGithubPrNotFound() throws Exception {
        when(analysisService.analyze(any(), any()))
                .thenThrow(new GitHubApiException(ErrorCode.GITHUB_PR_NOT_FOUND,
                        "GitHub PR 不存在", "确认链接正确", false));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/o/r/pull/999"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GITHUB_PR_NOT_FOUND"))
                .andExpect(jsonPath("$.suggestion").value("确认链接正确"))
                .andExpect(jsonPath("$.retryable").value(false));
    }

    @Test
    void publishCommentReturnsGithubTokenRequired() throws Exception {
        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 1,
                "https://github.com/o/r/pull/1");
        when(parser.parse("https://github.com/o/r/pull/1")).thenReturn(ref);
        when(commentFormatter.format(any())).thenReturn("md");
        when(commentPublisher.publish(ref, "md"))
                .thenThrow(new GitHubApiException(ErrorCode.GITHUB_TOKEN_REQUIRED,
                        "发布 PR 评论需要配置 GITHUB_TOKEN",
                        "设置 GITHUB_TOKEN 后重试", false));

        mockMvc.perform(post("/api/reviews/publish-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/o/r/pull/1",
                                  "analysis": {
                                    "owner": "o", "repo": "r", "pullNumber": 1,
                                    "title": "T", "author": "a", "state": "open",
                                    "baseBranch": "main", "headBranch": "feat",
                                    "totalFiles": 1, "totalAdditions": 1,
                                    "totalDeletions": 0, "totalChanges": 1,
                                    "truncated": false, "truncationReason": null,
                                    "review": {"summary": "OK", "riskLevel": "LOW",
                                      "risks": [], "suggestions": [], "model": "m"}
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GITHUB_TOKEN_REQUIRED"))
                .andExpect(jsonPath("$.retryable").value(false));
    }
}
