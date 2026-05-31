package com.example.ai_review.cli;

import com.example.ai_review.diff.AnalysisMode;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record CliArguments(
        boolean enabled,
        boolean help,
        String prUrl,
        Path diffFile,
        boolean diffStdin,
        String repository,
        String baseBranch,
        String headBranch,
        AnalysisMode analysisMode
) {

    public static boolean isCliMode(String[] args) {
        for (String arg : args) {
            if ("--cli".equals(arg) || "--help".equals(arg) || "-h".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static CliArguments parse(String[] args) {
        if (!isCliMode(args)) {
            return new CliArguments(false, false, null, null, false,
                    "local-project", "main", "working-tree", AnalysisMode.FAST);
        }

        Map<String, String> values = new HashMap<>();
        boolean enabled = false;
        boolean help = false;
        boolean diffStdin = false;

        for (String arg : args) {
            if ("--cli".equals(arg)) {
                enabled = true;
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                help = true;
                enabled = true;
            } else if ("--diff-stdin".equals(arg)) {
                diffStdin = true;
            } else if (arg.startsWith("--") && arg.contains("=")) {
                int split = arg.indexOf('=');
                values.put(arg.substring(2, split), arg.substring(split + 1));
            } else if (arg.startsWith("--")) {
                throw new IllegalArgumentException("未知 CLI 参数：" + arg);
            }
        }

        String prUrl = blankToNull(values.get("pr"));
        Path diffFile = values.containsKey("diff-file") ? Path.of(values.get("diff-file")) : null;
        AnalysisMode mode = parseMode(values.get("mode"));

        CliArguments parsed = new CliArguments(
                enabled,
                help,
                prUrl,
                diffFile,
                diffStdin,
                defaultIfBlank(values.get("repository"), "local-project"),
                defaultIfBlank(values.get("base"), "main"),
                defaultIfBlank(values.get("head"), "working-tree"),
                mode
        );

        if (enabled && !help) {
            parsed.validate();
        }
        return parsed;
    }

    private void validate() {
        int sources = 0;
        if (prUrl != null) sources++;
        if (diffFile != null) sources++;
        if (diffStdin) sources++;

        if (sources == 0) {
            throw new IllegalArgumentException("CLI 模式必须提供 --pr、--diff-file 或 --diff-stdin 其中一种输入");
        }
        if (sources > 1) {
            throw new IllegalArgumentException("CLI 模式一次只能选择一种输入：--pr、--diff-file 或 --diff-stdin");
        }
    }

    private static AnalysisMode parseMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return AnalysisMode.FAST;
        }
        try {
            return AnalysisMode.valueOf(rawMode.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("analysis mode 只支持 FAST 或 DEEP：" + rawMode);
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
