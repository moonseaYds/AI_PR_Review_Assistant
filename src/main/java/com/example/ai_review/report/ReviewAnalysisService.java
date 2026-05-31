package com.example.ai_review.report;

import com.example.ai_review.diff.AnalyzeDiffRequest;
import com.example.ai_review.diff.AnalysisMode;
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
import com.example.ai_review.review.RiskItem;
import com.example.ai_review.review.RiskLevel;
import com.example.ai_review.review.SuggestionItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewAnalysisService {

    private static final int DEEP_BATCH_TARGET_PATCH_CHARS = 16000;
    private static final int DEEP_BATCH_FILE_PATCH_CHARS = 8000;
    private static final int DEEP_BATCH_MAX_COUNT = 6;

    private final GitHubPullRequestUrlParser parser;
    private final GitHubPrFetcher fetcher;
    private final DiffContextBuilder diffContextBuilder;
    private final DeepSeekClient deepSeekClient;
    private final ReviewPromptBuilder promptBuilder;
    private final LocalDiffParser localDiffParser;
    private final MergeRiskAnalyzer mergeRiskAnalyzer;

    public ReviewAnalysisService(GitHubPullRequestUrlParser parser,
                                  GitHubPrFetcher fetcher,
                                  DiffContextBuilder diffContextBuilder,
                                  DeepSeekClient deepSeekClient,
                                  ReviewPromptBuilder promptBuilder,
                                  LocalDiffParser localDiffParser,
                                  MergeRiskAnalyzer mergeRiskAnalyzer) {
        this.parser = parser;
        this.fetcher = fetcher;
        this.diffContextBuilder = diffContextBuilder;
        this.deepSeekClient = deepSeekClient;
        this.promptBuilder = promptBuilder;
        this.localDiffParser = localDiffParser;
        this.mergeRiskAnalyzer = mergeRiskAnalyzer;
    }

    public AnalyzePullRequestResponse analyze(String prUrl) {
        return analyze(prUrl, AnalysisMode.FAST);
    }

    public AnalyzePullRequestResponse analyze(String prUrl, AnalysisMode analysisMode) {
        AnalysisMode mode = AnalysisMode.defaultIfNull(analysisMode);
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
                fetchResult.changedFiles(),
                mode
        );

        DiffReviewContext diffContext = diffContextBuilder.build(buildRequest);

        ReviewExecutionResult reviewResult = review(buildRequest, diffContext);
        MergeRiskReport mergeRisk = mergeRiskAnalyzer.analyze(fetchResult.changedFiles());

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
                diffContext.analysisMode(),
                diffContext.contextStrategy(),
                reviewResult.batchReview(),
                reviewResult.reviewBatches(),
                reviewResult.batchStrategy(),
                mergeRisk,
                reviewResult.report()
        );
    }

    public AnalyzePullRequestResponse analyzeDiff(AnalyzeDiffRequest request) {
        // 解析本地 diff 文本为 ChangedFile 列表，不调用 GitHub
        List<ChangedFile> changedFiles = localDiffParser.parse(request.diffText());

        String repo = defaultIfBlank(request.repository(), "local-project");
        String baseBranch = defaultIfBlank(request.baseBranch(), "main");
        String headBranch = defaultIfBlank(request.headBranch(), "working-tree");

        BuildDiffContextRequest buildRequest = new BuildDiffContextRequest(
                "local", repo, 0, "Local Diff Review", changedFiles,
                AnalysisMode.defaultIfNull(request.analysisMode()));

        DiffReviewContext diffContext = diffContextBuilder.build(buildRequest);

        ReviewExecutionResult reviewResult = review(buildRequest, diffContext);
        MergeRiskReport mergeRisk = mergeRiskAnalyzer.analyze(changedFiles);

        return new AnalyzePullRequestResponse(
                "local", repo, 0, "Local Diff Review",
                "local", "local", baseBranch, headBranch,
                diffContext.totalFiles(),
                diffContext.totalAdditions(),
                diffContext.totalDeletions(),
                diffContext.totalChanges(),
                diffContext.truncated(),
                diffContext.truncationReason(),
                diffContext.analysisMode(),
                diffContext.contextStrategy(),
                reviewResult.batchReview(),
                reviewResult.reviewBatches(),
                reviewResult.batchStrategy(),
                mergeRisk,
                reviewResult.report()
        );
    }

    private ReviewExecutionResult review(BuildDiffContextRequest request, DiffReviewContext diffContext) {
        if (diffContext.analysisMode() == AnalysisMode.DEEP) {
            List<List<ChangedFile>> batches = splitIntoDeepBatches(request.changedFiles());
            if (batches.size() > 1) {
                return batchReview(request, batches);
            }
        }
        return singleReview(diffContext);
    }

    private ReviewExecutionResult singleReview(DiffReviewContext diffContext) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(diffContext);
        String rawResponse = deepSeekClient.chat(systemPrompt, userPrompt);
        ReviewReport parsedReport = deepSeekClient.parseReviewReport(rawResponse);
        return new ReviewExecutionResult(withModel(parsedReport), false, 1, null);
    }

    private ReviewExecutionResult batchReview(BuildDiffContextRequest request,
                                              List<List<ChangedFile>> batches) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        List<ReviewReport> batchReports = new ArrayList<>();

        for (int i = 0; i < batches.size(); i++) {
            BuildDiffContextRequest batchRequest = new BuildDiffContextRequest(
                    request.owner(),
                    request.repo(),
                    request.pullNumber(),
                    request.title() + "（批次 " + (i + 1) + "/" + batches.size() + "）",
                    batches.get(i),
                    AnalysisMode.DEEP
            );
            DiffReviewContext batchContext = diffContextBuilder.build(batchRequest);
            String userPrompt = promptBuilder.buildBatchUserPrompt(batchContext, i + 1, batches.size());
            String rawResponse = deepSeekClient.chat(systemPrompt, userPrompt);
            batchReports.add(deepSeekClient.parseReviewReport(rawResponse));
        }

        String strategy = "DEEP 分批 Review：将 diff 拆分为 " + batches.size()
                + " 批分别调用模型，再由后端合并风险点、建议和最高风险等级；每批目标约 "
                + DEEP_BATCH_TARGET_PATCH_CHARS + " 字符，最多 " + DEEP_BATCH_MAX_COUNT + " 批";
        return new ReviewExecutionResult(aggregate(batchReports), true, batches.size(), strategy);
    }

    private List<List<ChangedFile>> splitIntoDeepBatches(List<ChangedFile> files) {
        List<List<ChangedFile>> batches = new ArrayList<>();
        List<ChangedFile> current = new ArrayList<>();
        int currentChars = 0;

        for (ChangedFile file : files) {
            int fileChars = estimatedPatchChars(file);
            boolean canStartNewBatch = !current.isEmpty()
                    && currentChars + fileChars > DEEP_BATCH_TARGET_PATCH_CHARS
                    && batches.size() < DEEP_BATCH_MAX_COUNT - 1;
            if (canStartNewBatch) {
                batches.add(current);
                current = new ArrayList<>();
                currentChars = 0;
            }
            current.add(file);
            currentChars += fileChars;
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    private int estimatedPatchChars(ChangedFile file) {
        if (file.patch() == null || file.patch().isBlank()) {
            return 0;
        }
        return Math.min(file.patch().length(), DEEP_BATCH_FILE_PATCH_CHARS);
    }

    private ReviewReport aggregate(List<ReviewReport> reports) {
        List<RiskItem> risks = new ArrayList<>();
        List<SuggestionItem> suggestions = new ArrayList<>();
        RiskLevel highestRisk = RiskLevel.LOW;
        StringBuilder summary = new StringBuilder("DEEP 分批分析完成，共 ")
                .append(reports.size()).append(" 批。");

        for (int i = 0; i < reports.size(); i++) {
            ReviewReport report = reports.get(i);
            highestRisk = maxRisk(highestRisk, report.riskLevel());
            if (report.risks() != null) {
                risks.addAll(report.risks());
            }
            if (report.suggestions() != null) {
                suggestions.addAll(report.suggestions());
            }
            summary.append(" 批次 ").append(i + 1).append("：")
                    .append(compactSummary(report.summary()));
        }

        summary.append(" 整体最高风险等级为 ").append(highestRisk).append("。");
        return new ReviewReport(summary.toString(), highestRisk, risks, suggestions, deepSeekClient.getModel());
    }

    private ReviewReport withModel(ReviewReport report) {
        return new ReviewReport(
                report.summary(),
                report.riskLevel(),
                report.risks(),
                report.suggestions(),
                deepSeekClient.getModel()
        );
    }

    private RiskLevel maxRisk(RiskLevel current, RiskLevel candidate) {
        if (candidate == null) {
            return current;
        }
        return riskRank(candidate) > riskRank(current) ? candidate : current;
    }

    private int riskRank(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private String compactSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "无摘要。";
        }
        String normalized = summary.replaceAll("\\s+", " ").strip();
        if (normalized.length() > 120) {
            return normalized.substring(0, 120) + "...";
        }
        if (normalized.endsWith("。") || normalized.endsWith("！") || normalized.endsWith("？")) {
            return normalized;
        }
        return normalized + "。";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private record ReviewExecutionResult(ReviewReport report,
                                         boolean batchReview,
                                         int reviewBatches,
                                         String batchStrategy) {
    }
}
