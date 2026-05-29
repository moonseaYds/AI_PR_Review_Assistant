package com.example.ai_review.diff;

import com.example.ai_review.github.ChangedFile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DiffContextBuilder {

    static final int MAX_FILE_PATCH_EXCERPT = 4000;
    static final int MAX_TOTAL_PATCH_EXCERPT = 16000;
    static final String NO_PATCH_PLACEHOLDER = "GitHub 未返回 patch，可能是二进制文件或变更过大";

    public DiffReviewContext build(BuildDiffContextRequest request) {
        List<ChangedFile> files = request.changedFiles();
        List<FileContext> fileContexts = new ArrayList<>();
        int totalPatchChars = 0;
        boolean overallTruncated = false;
        String truncationReason = null;

        for (ChangedFile file : files) {
            boolean fileTruncated = false;
            String excerpt;

            if (file.patch() == null || file.patch().isBlank()) {
                excerpt = NO_PATCH_PLACEHOLDER;
            } else {
                String patch = file.patch();
                int remainingBudget = MAX_TOTAL_PATCH_EXCERPT - totalPatchChars;

                if (remainingBudget <= 0) {
                    // Total budget exhausted — skip this file's patch entirely
                    excerpt = NO_PATCH_PLACEHOLDER;
                    fileTruncated = true;
                    overallTruncated = true;
                    if (truncationReason == null) {
                        truncationReason = "总 patch 长度超过限制（" + MAX_TOTAL_PATCH_EXCERPT + " 字符），已截断后续文件的 patch";
                    }
                } else if (patch.length() > Math.min(MAX_FILE_PATCH_EXCERPT, remainingBudget)) {
                    // Patch too long — need to truncate
                    int cutAt = Math.min(MAX_FILE_PATCH_EXCERPT, remainingBudget);
                    excerpt = patch.substring(0, cutAt);
                    fileTruncated = true;
                    overallTruncated = true;
                    if (patch.length() > MAX_FILE_PATCH_EXCERPT) {
                        truncationReason = "部分文件 patch 超过单文件限制（" + MAX_FILE_PATCH_EXCERPT + " 字符），已截断";
                    }
                    if (totalPatchChars + cutAt >= MAX_TOTAL_PATCH_EXCERPT) {
                        truncationReason = "总 patch 长度超过限制（" + MAX_TOTAL_PATCH_EXCERPT + " 字符），已截断后续文件的 patch";
                    }
                } else {
                    excerpt = patch;
                }

                totalPatchChars += Math.min(excerpt.length(), remainingBudget);
            }

            fileContexts.add(new FileContext(
                    file.filename(),
                    file.status(),
                    file.additions(),
                    file.deletions(),
                    file.changes(),
                    excerpt,
                    fileTruncated
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
                fileContexts
        );
    }
}
