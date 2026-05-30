package com.example.ai_review.report;

import com.example.ai_review.diff.AnalyzeDiffRequest;
import com.example.ai_review.diff.BuildDiffContextRequest;
import com.example.ai_review.diff.DiffContextBuilder;
import com.example.ai_review.diff.DiffReviewContext;
import com.example.ai_review.diff.LocalDiffParser;
import com.example.ai_review.github.ChangedFile;
import com.example.ai_review.github.GitHubPrFetcher;
import com.example.ai_review.github.GitHubPullRequestRef;
import com.example.ai_review.github.GitHubPullRequestUrlParser;
import com.example.ai_review.github.PrFetchResult;
import com.example.ai_review.review.DeepSeekClient;
import com.example.ai_review.review.ReviewPromptBuilder;
import com.example.ai_review.review.ReviewReport;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewAnalysisService {

    private final GitHubPullRequestUrlParser parser;
    private final GitHubPrFetcher fetcher;
    private final DiffContextBuilder diffContextBuilder;
    private final DeepSeekClient deepSeekClient;
    private final ReviewPromptBuilder promptBuilder;
    private final LocalDiffParser localDiffParser;

    public ReviewAnalysisService(GitHubPullRequestUrlParser parser,
                                  GitHubPrFetcher fetcher,
                                  DiffContextBuilder diffContextBuilder,
                                  DeepSeekClient deepSeekClient,
                                  ReviewPromptBuilder promptBuilder,
                                  LocalDiffParser localDiffParser) {
        this.parser = parser;
        this.fetcher = fetcher;
        this.diffContextBuilder = diffContextBuilder;
        this.deepSeekClient = deepSeekClient;
        this.promptBuilder = promptBuilder;
        this.localDiffParser = localDiffParser;
    }

    public AnalyzePullRequestResponse analyze(String prUrl) {
        GitHubPullRequestRef ref = parser.parse(prUrl);
        PrFetchResult fetchResult = fetcher.fetch(ref);

        if (fetchResult.changedFiles() == null || fetchResult.changedFiles().isEmpty()) {
            throw new IllegalArgumentException(
                    "该 PR 没有变更文件，无法进行 AI Review");
        }

        BuildDiffContextRequest buildRequest = new BuildDiffContextRequest(
                fetchResult.owner(),
                fetchResult.repo(),
                fetchResult.pullNumber(),
                fetchResult.title(),
                fetchResult.changedFiles()
        );

        DiffReviewContext diffContext = diffContextBuilder.build(buildRequest);

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(diffContext);
        String rawResponse = deepSeekClient.chat(systemPrompt, userPrompt);

        ReviewReport parsedReport = deepSeekClient.parseReviewReport(rawResponse);
        // 补全模型名：Prompt 不要求 DeepSeek 返回 model 字段，使用配置值
        ReviewReport report = new ReviewReport(
                parsedReport.summary(),
                parsedReport.riskLevel(),
                parsedReport.risks(),
                parsedReport.suggestions(),
                deepSeekClient.getModel()
        );

        return new AnalyzePullRequestResponse(
                fetchResult.owner(),
                fetchResult.repo(),
                fetchResult.pullNumber(),
                fetchResult.title(),
                fetchResult.author(),
                fetchResult.state(),
                fetchResult.baseBranch(),
                fetchResult.headBranch(),
                diffContext.totalFiles(),
                diffContext.totalAdditions(),
                diffContext.totalDeletions(),
                diffContext.totalChanges(),
                diffContext.truncated(),
                diffContext.truncationReason(),
                report
        );
    }

    public AnalyzePullRequestResponse analyzeDiff(AnalyzeDiffRequest request) {
        // 解析本地 diff 文本为 ChangedFile 列表，不调用 GitHub
        List<ChangedFile> changedFiles = localDiffParser.parse(request.diffText());

        String repo = defaultIfBlank(request.repository(), "local-project");
        String baseBranch = defaultIfBlank(request.baseBranch(), "main");
        String headBranch = defaultIfBlank(request.headBranch(), "working-tree");

        BuildDiffContextRequest buildRequest = new BuildDiffContextRequest(
                "local", repo, 0, "Local Diff Review", changedFiles);

        DiffReviewContext diffContext = diffContextBuilder.build(buildRequest);

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(diffContext);
        String rawResponse = deepSeekClient.chat(systemPrompt, userPrompt);

        ReviewReport parsedReport = deepSeekClient.parseReviewReport(rawResponse);
        ReviewReport report = new ReviewReport(
                parsedReport.summary(),
                parsedReport.riskLevel(),
                parsedReport.risks(),
                parsedReport.suggestions(),
                deepSeekClient.getModel()
        );

        return new AnalyzePullRequestResponse(
                "local", repo, 0, "Local Diff Review",
                "local", "local", baseBranch, headBranch,
                diffContext.totalFiles(),
                diffContext.totalAdditions(),
                diffContext.totalDeletions(),
                diffContext.totalChanges(),
                diffContext.truncated(),
                diffContext.truncationReason(),
                report
        );
    }

    private String defaultIfBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
