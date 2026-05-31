package com.example.ai_review.report;

import com.example.ai_review.diff.AnalyzeDiffRequest;
import com.example.ai_review.diff.AnalysisMode;
import com.example.ai_review.diff.BuildDiffContextRequest;
import com.example.ai_review.diff.DiffContextBuilder;
import com.example.ai_review.diff.DiffReviewContext;
import com.example.ai_review.diff.FileContext;
import com.example.ai_review.diff.LocalDiffParser;
import com.example.ai_review.github.*;
import com.example.ai_review.review.AiReviewModelClient;
import com.example.ai_review.review.ReviewPromptBuilder;
import com.example.ai_review.review.ReviewReport;
import com.example.ai_review.review.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewAnalysisServiceTest {

    @Mock
    private GitHubPullRequestUrlParser parser;

    @Mock
    private GitHubPrFetcher fetcher;

    @Mock
    private DiffContextBuilder diffContextBuilder;

    @Mock
    private AiReviewModelClient aiModelClient;

    @Mock
    private ReviewPromptBuilder promptBuilder;

    @Mock
    private LocalDiffParser localDiffParser;

    @Mock
    private MergeRiskAnalyzer mergeRiskAnalyzer;

    @InjectMocks
    private ReviewAnalysisService service;

    @Test
    void returnsFullAnalysisResponse() {
        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1,
                "https://github.com/owner/repo/pull/1");
        when(parser.parse("https://github.com/owner/repo/pull/1")).thenReturn(ref);

        ChangedFile file = new ChangedFile("App.java", "modified", 5, 2, 7, "@@ diff");
        PrFetchResult fetchResult = new PrFetchResult("owner", "repo", 1,
                "Fix bug", "octocat", "open", "main", "feature/fix",
                List.of(file));
        when(fetcher.fetch(eq(ref), any())).thenReturn(fetchResult);

        DiffReviewContext diffContext = new DiffReviewContext(
                "owner", "repo", 1, "Fix bug", 1, 5, 2, 7, false, null,
                List.of(new FileContext("App.java", "modified", 5, 2, 7,
                        "@@ diff", false)));
        when(diffContextBuilder.build(any(BuildDiffContextRequest.class))).thenReturn(diffContext);

        when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("user prompt");

        String rawJson = """
                {
                  "summary": "代码质量良好",
                  "riskLevel": "LOW",
                  "risks": [],
                  "suggestions": []
                }
                """;
        when(aiModelClient.chat(eq("system prompt"), eq("user prompt"), any())).thenReturn(rawJson);

        ReviewReport parsedReport = new ReviewReport(
                "代码质量良好", RiskLevel.LOW, List.of(), List.of(), null);
        when(aiModelClient.parseReviewReport(rawJson)).thenReturn(parsedReport);
        when(aiModelClient.getModel()).thenReturn("deepseek-v4-flash");
        when(mergeRiskAnalyzer.analyze(List.of(file))).thenReturn(
                new MergeRiskReport(RiskLevel.LOW, "未检测到明显合并风险", List.of()));

        AnalyzePullRequestResponse response = service.analyze(
                "https://github.com/owner/repo/pull/1");

        assertEquals("owner", response.owner());
        assertEquals("repo", response.repo());
        assertEquals(1, response.pullNumber());
        assertEquals("Fix bug", response.title());
        assertEquals("octocat", response.author());
        assertEquals("open", response.state());
        assertEquals("main", response.baseBranch());
        assertEquals("feature/fix", response.headBranch());
        assertEquals(1, response.totalFiles());
        assertEquals(5, response.totalAdditions());
        assertEquals(2, response.totalDeletions());
        assertEquals(7, response.totalChanges());
        assertFalse(response.truncated());
        assertNull(response.truncationReason());
        assertEquals(RiskLevel.LOW, response.mergeRisk().riskLevel());
        assertEquals("代码质量良好", response.review().summary());
        assertEquals(RiskLevel.LOW, response.review().riskLevel());
    }

    @Test
    void throwsOnEmptyChangedFiles() {
        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1,
                "https://github.com/owner/repo/pull/1");
        when(parser.parse("https://github.com/owner/repo/pull/1")).thenReturn(ref);

        PrFetchResult fetchResult = new PrFetchResult("owner", "repo", 1,
                "Empty PR", "octocat", "open", "main", "feature/empty",
                List.of());
        when(fetcher.fetch(eq(ref), any())).thenReturn(fetchResult);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.analyze("https://github.com/owner/repo/pull/1"));
        assertTrue(ex.getMessage().contains("没有变更文件"));
    }

    @Test
    void fillsModelFromClientWhenNull() {
        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 1,
                "https://github.com/o/r/pull/1");
        when(parser.parse("https://github.com/o/r/pull/1")).thenReturn(ref);

        ChangedFile file = new ChangedFile("A.java", "modified", 1, 0, 1, "@@ diff");
        PrFetchResult fetchResult = new PrFetchResult("o", "r", 1,
                "PR", "author", "open", "main", "feat", List.of(file));
        when(fetcher.fetch(eq(ref), any())).thenReturn(fetchResult);

        DiffReviewContext diffContext = new DiffReviewContext(
                "o", "r", 1, "PR", 1, 1, 0, 1, false, null,
                List.of(new FileContext("A.java", "modified", 1, 0, 1,
                        "@@ diff", false)));
        when(diffContextBuilder.build(any(BuildDiffContextRequest.class))).thenReturn(diffContext);

        when(promptBuilder.buildSystemPrompt()).thenReturn("s");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("u");

        String rawJson = """
                {
                  "summary": "OK",
                  "riskLevel": "MEDIUM",
                  "risks": [{"file":"A.java","level":"MEDIUM","title":"risk","reason":"r","suggestion":"s"}],
                  "suggestions": []
                }
                """;
        when(aiModelClient.chat(eq("s"), eq("u"), any())).thenReturn(rawJson);

        // parseReviewReport 返回 model=null，模拟 DeepSeek JSON 不含 model 字段
        ReviewReport parsedWithoutModel = new ReviewReport(
                "OK", RiskLevel.MEDIUM,
                List.of(new com.example.ai_review.review.RiskItem(
                        "A.java", "MEDIUM", "risk", "r", "s", null, null, null)),
                List.of(), null);
        when(aiModelClient.parseReviewReport(rawJson)).thenReturn(parsedWithoutModel);
        when(aiModelClient.getModel()).thenReturn("deepseek-v4-flash");
        when(mergeRiskAnalyzer.analyze(List.of(file))).thenReturn(
                new MergeRiskReport(RiskLevel.MEDIUM, "检测到合并风险", List.of()));

        AnalyzePullRequestResponse response = service.analyze(
                "https://github.com/o/r/pull/1");

        assertEquals("deepseek-v4-flash", response.review().model());
        assertEquals("OK", response.review().summary());
        assertEquals(RiskLevel.MEDIUM, response.review().riskLevel());
        assertEquals(1, response.review().risks().size());
    }

    @Test
    void analyzeDiffUsesLocalLabels() {
        String diffText = "diff --git a/A.java b/A.java\n--- a/A.java\n+++ b/A.java\n@@ -1 +1 @@\n-old\n+new";
        ChangedFile file = new ChangedFile("A.java", "modified", 1, 1, 2, diffText);
        when(localDiffParser.parse(diffText)).thenReturn(List.of(file));

        DiffReviewContext diffContext = new DiffReviewContext(
                "local", "local-project", 0, "Local Diff Review", 1, 1, 1, 2, false, null,
                List.of(new FileContext("A.java", "modified", 1, 1, 2, diffText, false)));
        when(diffContextBuilder.build(any(BuildDiffContextRequest.class))).thenReturn(diffContext);

        when(promptBuilder.buildSystemPrompt()).thenReturn("s");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("u");
        when(aiModelClient.chat(eq("s"), eq("u"), any())).thenReturn("""
                {"summary":"OK","riskLevel":"LOW","risks":[],"suggestions":[]}""");
        when(aiModelClient.parseReviewReport(any()))
                .thenReturn(new ReviewReport("OK", RiskLevel.LOW, List.of(), List.of(), null));
        when(aiModelClient.getModel()).thenReturn("deepseek-v4-flash");
        when(mergeRiskAnalyzer.analyze(List.of(file))).thenReturn(
                new MergeRiskReport(RiskLevel.LOW, "本地 diff 合并风险较低", List.of()));

        // Use all blank optional fields to verify defaults
        AnalyzePullRequestResponse response = service.analyzeDiff(
                new AnalyzeDiffRequest("", "  ", null, diffText));

        assertEquals("local-project", response.repo());
        assertEquals("main", response.baseBranch());
        assertEquals("working-tree", response.headBranch());

        // 验证 analyzeDiff 不调用 GitHub parser/fetcher
        verifyNoInteractions(parser, fetcher);
    }

    @Test
    void deepModeUsesBatchReviewForLargeDiff() {
        ReviewAnalysisService batchService = new ReviewAnalysisService(
                parser, fetcher, new DiffContextBuilder(), aiModelClient, promptBuilder,
                localDiffParser, new MergeRiskAnalyzer());

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 2,
                "https://github.com/o/r/pull/2");
        when(parser.parse("https://github.com/o/r/pull/2")).thenReturn(ref);

        List<ChangedFile> files = List.of(
                new ChangedFile("src/main/java/AController.java", "modified", 100, 0, 100, "a".repeat(9000)),
                new ChangedFile("src/main/java/BService.java", "modified", 100, 0, 100, "b".repeat(9000)),
                new ChangedFile("src/main/java/SecurityConfig.java", "modified", 100, 0, 100, "c".repeat(9000))
        );
        when(fetcher.fetch(eq(ref), any())).thenReturn(new PrFetchResult(
                "o", "r", 2, "Large PR", "dev", "open", "main", "feat/batch", files));

        when(promptBuilder.buildSystemPrompt()).thenReturn("s");
        when(promptBuilder.buildBatchUserPrompt(any(), anyInt(), anyInt()))
                .thenReturn("u1", "u2");
        when(aiModelClient.chat(eq("s"), eq("u1"), any())).thenReturn("raw1");
        when(aiModelClient.chat(eq("s"), eq("u2"), any())).thenReturn("raw2");
        when(aiModelClient.parseReviewReport("raw1"))
                .thenReturn(new ReviewReport("第一批未发现明显风险", RiskLevel.LOW, List.of(), List.of(), null));
        when(aiModelClient.parseReviewReport("raw2"))
                .thenReturn(new ReviewReport("第二批发现安全配置风险", RiskLevel.HIGH,
                        List.of(new com.example.ai_review.review.RiskItem(
                                "src/main/java/SecurityConfig.java", "HIGH", "权限放开",
                                "permitAll 可能扩大访问范围", "收紧权限", null, null, null)),
                        List.of(), null));
        when(aiModelClient.getModel()).thenReturn("deepseek-v4-flash");

        AnalyzePullRequestResponse response = batchService.analyze(
                "https://github.com/o/r/pull/2", AnalysisMode.DEEP);

        assertTrue(response.batchReview());
        assertEquals(2, response.reviewBatches());
        assertTrue(response.batchStrategy().contains("DEEP 分批 Review"));
        assertEquals(RiskLevel.HIGH, response.mergeRisk().riskLevel());
        assertEquals(RiskLevel.HIGH, response.review().riskLevel());
        assertEquals(1, response.review().risks().size());
        assertTrue(response.review().summary().contains("共 2 批"));
        verify(aiModelClient, times(2)).chat(any(), any(), any());
        verify(promptBuilder, times(2)).buildBatchUserPrompt(any(), anyInt(), anyInt());
    }
}
