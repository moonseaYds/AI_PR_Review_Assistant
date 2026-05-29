package com.example.ai_review.diff;

import com.example.ai_review.github.ChangedFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Request DTO for POST /api/reviews/build-diff-context.
 * Compatible with a subset of PrFetchResult fields.
 */
public record BuildDiffContextRequest(
        @NotBlank(message = "不能为空")
        String owner,

        @NotBlank(message = "不能为空")
        String repo,

        @Positive(message = "必须大于 0")
        int pullNumber,

        @NotBlank(message = "不能为空")
        String title,

        @NotEmpty(message = "不能为空")
        List<@Valid ChangedFile> changedFiles
) {
}
