package com.example.ai_review.cli;

import com.example.ai_review.diff.AnalyzeDiffRequest;
import com.example.ai_review.report.AnalyzePullRequestResponse;
import com.example.ai_review.report.ReviewAnalysisService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Component
public class CliReviewRunner implements ApplicationRunner {

    private final ApplicationContext applicationContext;
    private final ReviewAnalysisService reviewAnalysisService;
    private final CliReportFormatter formatter;

    public CliReviewRunner(ApplicationContext applicationContext,
                           ReviewAnalysisService reviewAnalysisService,
                           CliReportFormatter formatter) {
        this.applicationContext = applicationContext;
        this.reviewAnalysisService = reviewAnalysisService;
        this.formatter = formatter;
    }

    @Override
    public void run(ApplicationArguments args) {
        CliArguments cliArguments = CliArguments.parse(args.getSourceArgs());
        if (!cliArguments.enabled()) {
            return;
        }

        int exitCode = 0;
        try {
            if (cliArguments.help()) {
                System.out.println(usage());
            } else {
                AnalyzePullRequestResponse response = analyze(cliArguments);
                System.out.println(formatter.format(response));
            }
        } catch (Exception ex) {
            exitCode = 1;
            System.err.println("AI PR Review CLI 执行失败：" + ex.getMessage());
        } finally {
            int finalExitCode = exitCode;
            int springExitCode = SpringApplication.exit(applicationContext, () -> finalExitCode);
            if (springExitCode != 0) {
                System.exit(springExitCode);
            }
        }
    }

    private AnalyzePullRequestResponse analyze(CliArguments args) throws IOException {
        if (args.prUrl() != null) {
            return reviewAnalysisService.analyze(args.prUrl(), args.analysisMode());
        }

        String diffText;
        if (args.diffFile() != null) {
            diffText = Files.readString(args.diffFile(), StandardCharsets.UTF_8);
        } else {
            diffText = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        }

        AnalyzeDiffRequest request = new AnalyzeDiffRequest(
                args.repository(),
                args.baseBranch(),
                args.headBranch(),
                args.analysisMode(),
                diffText
        );
        return reviewAnalysisService.analyzeDiff(request);
    }

    private String usage() {
        return """
                AI PR Review Assistant CLI

                Usage:
                  java -jar target/Ai_Review-0.0.1-SNAPSHOT.jar --cli --pr=https://github.com/owner/repo/pull/123 [--mode=FAST|DEEP]
                  git diff main...HEAD | java -jar target/Ai_Review-0.0.1-SNAPSHOT.jar --cli --diff-stdin [--mode=FAST|DEEP]
                  java -jar target/Ai_Review-0.0.1-SNAPSHOT.jar --cli --diff-file=patch.diff [--repository=my-repo] [--base=main] [--head=working-tree]

                Notes:
                  - API Key 和 GitHub Token 请通过环境变量或 .env 配置，不建议放入命令行参数。
                  - FAST 模式响应更快、成本更低；DEEP 模式适合更大的 diff，但响应更慢。
                """;
    }
}
