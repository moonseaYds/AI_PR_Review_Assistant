package com.example.ai_review.diff;

import com.example.ai_review.github.ChangedFile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiffContextBuilder {

    static final int MAX_FILE_PATCH_EXCERPT = 4000;
    static final int MAX_TOTAL_PATCH_EXCERPT = 16000;
    static final int DEEP_MAX_FILE_PATCH_EXCERPT = 8000;
    static final int DEEP_MAX_TOTAL_PATCH_EXCERPT = 48000;
    static final String NO_PATCH_PLACEHOLDER = "GitHub 未返回 patch，可能是二进制文件或变更过大";

    public DiffReviewContext build(BuildDiffContextRequest request) {
        List<ChangedFile> files = request.changedFiles();
        AnalysisMode mode = AnalysisMode.defaultIfNull(request.analysisMode());
        int maxFileChars = maxFileChars(mode);
        int maxTotalChars = maxTotalChars(mode);
        String contextStrategy = contextStrategy(mode, maxFileChars, maxTotalChars);

        Map<Integer, PatchSelection> selectedPatches = selectPatches(files, mode, maxFileChars, maxTotalChars);

        List<FileContext> fileContexts = new ArrayList<>();
        boolean overallTruncated = false;
        String truncationReason = null;

        for (int i = 0; i < files.size(); i++) {
            ChangedFile file = files.get(i);
            PatchSelection selection = selectedPatches.get(i);

            if (selection.truncated()) {
                overallTruncated = true;
                if (truncationReason == null || selection.reasonPriority() >= reasonPriority(truncationReason)) {
                    truncationReason = selection.reason();
                }
            }

            fileContexts.add(new FileContext(
                    file.filename(),
                    file.status(),
                    file.additions(),
                    file.deletions(),
                    file.changes(),
                    selection.excerpt(),
                    selection.truncated()
            ));
        }

        int totalAdditions = files.stream().mapToInt(ChangedFile::additions).sum();
        int totalDeletions = files.stream().mapToInt(ChangedFile::deletions).sum();
        int totalChanges = files.stream().mapToInt(ChangedFile::changes).sum();

        return new DiffReviewContext(
                request.owner(),
                request.repo(),
                request.pullNumber(),
                request.title(),
                files.size(),
                totalAdditions,
                totalDeletions,
                totalChanges,
                overallTruncated,
                truncationReason,
                mode,
                contextStrategy,
                fileContexts
        );
    }

    private Map<Integer, PatchSelection> selectPatches(List<ChangedFile> files,
                                                       AnalysisMode mode,
                                                       int maxFileChars,
                                                       int maxTotalChars) {
        List<FileCandidate> candidates = new ArrayList<>();
        Map<Integer, PatchSelection> selections = new HashMap<>();

        for (int i = 0; i < files.size(); i++) {
            ChangedFile file = files.get(i);
            if (file.patch() == null || file.patch().isBlank()) {
                selections.put(i, new PatchSelection(NO_PATCH_PLACEHOLDER, false, null, 0));
            } else {
                candidates.add(new FileCandidate(i, file, riskScore(file)));
            }
        }

        int totalPatchChars = 0;
        List<FileCandidate> orderedCandidates = candidates.stream()
                .sorted(Comparator
                        .comparingInt(FileCandidate::riskScore).reversed()
                        .thenComparingInt(FileCandidate::index))
                .toList();

        for (FileCandidate candidate : orderedCandidates) {
            ChangedFile file = candidate.file();
            String patch = file.patch();
            int remainingBudget = maxTotalChars - totalPatchChars;

            if (remainingBudget <= 0) {
                selections.put(candidate.index(), new PatchSelection(
                        NO_PATCH_PLACEHOLDER,
                        true,
                        totalLimitReason(mode, maxTotalChars),
                        2
                ));
                continue;
            }

            int cutAt = Math.min(maxFileChars, remainingBudget);
            if (patch.length() > cutAt) {
                selections.put(candidate.index(), new PatchSelection(
                        patch.substring(0, cutAt),
                        true,
                        patch.length() > maxFileChars
                                ? fileLimitReason(mode, maxFileChars)
                                : totalLimitReason(mode, maxTotalChars),
                        patch.length() > maxFileChars ? 1 : 2
                ));
                totalPatchChars += cutAt;
            } else {
                selections.put(candidate.index(), new PatchSelection(patch, false, null, 0));
                totalPatchChars += patch.length();
            }
        }

        return selections;
    }

    private int riskScore(ChangedFile file) {
        String filename = file.filename() != null ? file.filename().toLowerCase() : "";
        String patch = file.patch() != null ? file.patch().toLowerCase() : "";
        int score = Math.max(0, file.changes());

        if (filename.contains("security") || filename.contains("auth")) score += 90;
        if (filename.contains("controller")) score += 70;
        if (filename.contains("service")) score += 65;
        if (filename.contains("config") || filename.contains("application.")) score += 60;
        if (filename.contains("exception") || filename.contains("error")) score += 55;
        if (filename.endsWith("pom.xml") || filename.endsWith("build.gradle")) score += 75;
        if (filename.endsWith(".sql") || filename.contains("mapper") || filename.contains("repository")) score += 45;
        if (filename.contains("test/") || filename.endsWith("test.java")) score -= 20;
        if (filename.endsWith(".md") || filename.endsWith(".png") || filename.endsWith(".jpg")) score -= 30;

        if (patch.contains("password") || patch.contains("secret") || patch.contains("token")) score += 70;
        if (patch.contains("permitall") || patch.contains("hasrole") || patch.contains("authorize")) score += 65;
        if (patch.contains("restclient") || patch.contains("webclient") || patch.contains("http://")) score += 45;
        if (patch.contains("fileinputstream") || patch.contains("files.") || patch.contains("delete")) score += 35;
        if (patch.contains("catch") || patch.contains("throw ") || patch.contains("exception")) score += 30;
        if (patch.contains("null") || patch.contains("optional")) score += 20;

        return score;
    }

    private int maxFileChars(AnalysisMode mode) {
        return mode == AnalysisMode.DEEP ? DEEP_MAX_FILE_PATCH_EXCERPT : MAX_FILE_PATCH_EXCERPT;
    }

    private int maxTotalChars(AnalysisMode mode) {
        return mode == AnalysisMode.DEEP ? DEEP_MAX_TOTAL_PATCH_EXCERPT : MAX_TOTAL_PATCH_EXCERPT;
    }

    private String contextStrategy(AnalysisMode mode, int maxFileChars, int maxTotalChars) {
        if (mode == AnalysisMode.DEEP) {
            return "DEEP 模式：使用更宽上下文预算模拟分批分析覆盖面，单文件限制 "
                    + maxFileChars + " 字符，总限制 " + maxTotalChars + " 字符";
        }
        return "FAST 模式：按风险权重优先保留关键文件上下文，单文件限制 "
                + maxFileChars + " 字符，总限制 " + maxTotalChars + " 字符";
    }

    private String fileLimitReason(AnalysisMode mode, int maxFileChars) {
        return mode.name() + " 模式下部分文件 patch 超过单文件限制（" + maxFileChars + " 字符），已截断";
    }

    private String totalLimitReason(AnalysisMode mode, int maxTotalChars) {
        return mode.name() + " 模式下总 patch 长度超过限制（" + maxTotalChars + " 字符），已按风险权重选择上下文";
    }

    private int reasonPriority(String reason) {
        return reason != null && reason.contains("总 patch") ? 2 : 1;
    }

    private record FileCandidate(int index, ChangedFile file, int riskScore) {
    }

    private record PatchSelection(String excerpt, boolean truncated, String reason, int reasonPriority) {
    }
}
