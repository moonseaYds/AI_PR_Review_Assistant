package com.example.ai_review.cli;

import com.example.ai_review.report.AnalyzePullRequestResponse;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CliReportRenderer {

    static final String OUTPUT_FORMAT_ENV = "AI_PR_REVIEW_OUTPUT_FORMAT";

    private final CliReportFormatter markdownFormatter;
    private final CliHtmlReportFormatter htmlFormatter;

    public CliReportRenderer(CliReportFormatter markdownFormatter,
                             CliHtmlReportFormatter htmlFormatter) {
        this.markdownFormatter = markdownFormatter;
        this.htmlFormatter = htmlFormatter;
    }

    public String format(AnalyzePullRequestResponse response) {
        return format(response, System.getenv(OUTPUT_FORMAT_ENV));
    }

    String format(AnalyzePullRequestResponse response, String rawFormat) {
        CliOutputFormat outputFormat = CliOutputFormat.from(rawFormat);
        return switch (outputFormat) {
            case MARKDOWN -> markdownFormatter.format(response);
            case HTML -> htmlFormatter.format(response);
        };
    }

    enum CliOutputFormat {
        MARKDOWN,
        HTML;

        static CliOutputFormat from(String rawFormat) {
            if (rawFormat == null || rawFormat.isBlank()) {
                return MARKDOWN;
            }
            return switch (rawFormat.strip().toLowerCase(Locale.ROOT)) {
                case "markdown", "md" -> MARKDOWN;
                case "html" -> HTML;
                default -> throw new IllegalArgumentException(
                        OUTPUT_FORMAT_ENV + " 只支持 markdown 或 html：" + rawFormat);
            };
        }
    }
}
