package com.example.ai_review.diff;

import com.example.ai_review.common.BadRequestException;
import com.example.ai_review.common.ErrorCode;
import com.example.ai_review.github.ChangedFile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocalDiffParser {

    private static final Pattern FILE_SEPARATOR = Pattern.compile("^diff --git ", Pattern.MULTILINE);
    private static final Pattern FILE_PATH = Pattern.compile("^diff --git a/(.*) b/(.*)$", Pattern.MULTILINE);
    private static final Pattern NEW_FILE = Pattern.compile("^new file mode", Pattern.MULTILINE);
    private static final Pattern DELETED_FILE = Pattern.compile("^deleted file mode", Pattern.MULTILINE);

    public List<ChangedFile> parse(String diffText) {
        if (diffText == null || diffText.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_DIFF_TEXT,
                    "diffText 不能为空", "请粘贴完整 git diff 输出");
        }

        String trimmed = diffText.strip();

        // Require at least one diff --git header
        if (!FILE_SEPARATOR.matcher(trimmed).find()) {
            throw new BadRequestException(ErrorCode.INVALID_DIFF_TEXT,
                    "无法解析 diff 内容：未找到标准的 git diff header (diff --git a/... b/...)",
                    "请粘贴 git diff 或 git diff main...HEAD 的完整输出");
        }

        List<ChangedFile> files = new ArrayList<>();
        String[] parts = FILE_SEPARATOR.split(trimmed);

        for (String part : parts) {
            String block = part.strip();
            if (block.isEmpty()) continue;

            // Prepend "diff --git " to the block for parsing
            String fullBlock = "diff --git " + block;

            Matcher pathMatcher = FILE_PATH.matcher(fullBlock);
            if (!pathMatcher.find()) continue;

            // b/ path for modified/added, fallback to a/ for deleted
            String filename = pathMatcher.group(2).isEmpty()
                    ? pathMatcher.group(1) : pathMatcher.group(2);

            String status;
            if (NEW_FILE.matcher(fullBlock).find()) {
                status = "added";
            } else if (DELETED_FILE.matcher(fullBlock).find()) {
                status = "removed";
            } else {
                status = "modified";
            }

            int additions = 0;
            int deletions = 0;
            // Count +/- lines excluding the header markers
            for (String line : fullBlock.split("\n")) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    additions++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    deletions++;
                }
            }

            files.add(new ChangedFile(
                    filename,
                    status,
                    additions,
                    deletions,
                    additions + deletions,
                    fullBlock
            ));
        }

        if (files.isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_DIFF_TEXT,
                    "无法解析 diff 内容：未找到有效的文件变更块",
                    "请确认粘贴的是完整 git diff 输出");
        }

        return files;
    }
}
