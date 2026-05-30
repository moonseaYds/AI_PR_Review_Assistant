package com.example.ai_review.report;

import com.example.ai_review.common.GitHubApiException;
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
        when(analysisService.analyze(any())).thenReturn(new AnalyzePullRequestResponse(
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
                .andExpect(jsonPath("$.review.summary").value("代码质量良好"))
                .andExpect(jsonPath("$.review.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.review.risks").isEmpty());
    }

    @Test
    void analyzeReturns400ForIllegalArgument() throws Exception {
        when(analysisService.analyze(any()))
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
    void analyzeReturns502ForGitHubError() throws Exception {
        when(analysisService.analyze(any()))
                .thenThrow(new GitHubApiException("GitHub PR 不存在"));

        mockMvc.perform(post("/api/reviews/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/owner/repo/pull/99999"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message", containsString("GitHub")));
    }

    @Test
    void analyzeReturns502ForDeepSeekError() throws Exception {
        when(analysisService.analyze(any()))
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
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message", containsString("DEEPSEEK_API_KEY")));
    }

    @Test
    void analyzeReturns400ForEmptyChangedFiles() throws Exception {
        when(analysisService.analyze(any()))
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
        when(analysisService.analyze(any())).thenReturn(new AnalyzePullRequestResponse(
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
                .andExpect(jsonPath("$.code").value("UPSTREAM_ERROR"))
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
                .thenThrow(new IllegalArgumentException("无法解析 diff 内容"));

        mockMvc.perform(post("/api/reviews/analyze-diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diffText": "not a valid diff"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
