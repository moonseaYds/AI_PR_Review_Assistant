package com.example.ai_review.diff;

import com.example.ai_review.github.ChangedFile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiffContextBuilderTest {

    private final DiffContextBuilder builder = new DiffContextBuilder();

    @Test
    void smallDiffNotTruncated() {
        BuildDiffContextRequest request = new BuildDiffContextRequest(
                "owner", "repo", 1, "Small PR",
                List.of(
                        new ChangedFile("A.java", "modified", 3, 1, 4, "@@ -1 +1 @@ small patch"),
                        new ChangedFile("B.java", "added", 10, 0, 10, "@@ -1 +1 @@ another patch")
                )
        );

        DiffReviewContext context = builder.build(request);

        assertThat(context.truncated()).isFalse();
        assertThat(context.truncationReason()).isNull();
        assertThat(context.analysisMode()).isEqualTo(AnalysisMode.FAST);
        assertThat(context.contextStrategy()).contains("FAST").contains("4000").contains("16000");
        assertThat(context.totalFiles()).isEqualTo(2);
        assertThat(context.totalAdditions()).isEqualTo(13);
        assertThat(context.totalDeletions()).isEqualTo(1);
        assertThat(context.totalChanges()).isEqualTo(14);
        assertThat(context.fileContexts()).hasSize(2);
        assertThat(context.fileContexts().get(0).patchTruncated()).isFalse();
        assertThat(context.fileContexts().get(1).patchTruncated()).isFalse();
        assertThat(context.fileContexts().get(0).patchExcerpt()).isEqualTo("@@ -1 +1 @@ small patch");
        assertThat(context.fileContexts().get(1).patchExcerpt()).isEqualTo("@@ -1 +1 @@ another patch");
    }

    @Test
    void singleFilePatchExceedsLimitIsTruncated() {
        String longPatch = "x".repeat(5000); // exceeds 4000
        BuildDiffContextRequest request = new BuildDiffContextRequest(
                "o", "r", 2, "Large File PR",
                List.of(
                        new ChangedFile("BigFile.java", "modified", 100, 50, 150, longPatch)
                )
        );

        DiffReviewContext context = builder.build(request);

        assertThat(context.truncated()).isTrue();
        assertThat(context.truncationReason()).contains("4000");
        assertThat(context.fileContexts()).hasSize(1);

        FileContext fc = context.fileContexts().get(0);
        assertThat(fc.patchTruncated()).isTrue();
        assertThat(fc.patchExcerpt()).hasSize(DiffContextBuilder.MAX_FILE_PATCH_EXCERPT);
        assertThat(fc.patchExcerpt()).isEqualTo(longPatch.substring(0, DiffContextBuilder.MAX_FILE_PATCH_EXCERPT));
    }

    @Test
    void multiFileTotalExceedsLimitIsTruncated() {
        // 5 files, each 5000 chars → per-file cap 4000, total would be 5x4000=20000 > 16000
        // Files 1-4: truncated to 4000 each. File 5: budget exhausted → placeholder.
        BuildDiffContextRequest request = new BuildDiffContextRequest(
                "o", "r", 3, "Many Files PR",
                List.of(
                        new ChangedFile("File1.java", "modified", 10, 5, 15, "a".repeat(5000)),
                        new ChangedFile("File2.java", "modified", 10, 5, 15, "b".repeat(5000)),
                        new ChangedFile("File3.java", "modified", 10, 5, 15, "c".repeat(5000)),
                        new ChangedFile("File4.java", "modified", 10, 5, 15, "d".repeat(5000)),
                        new ChangedFile("File5.java", "modified", 10, 5, 15, "e".repeat(5000))
                )
        );

        DiffReviewContext context = builder.build(request);

        assertThat(context.truncated()).isTrue();
        assertThat(context.truncationReason()).contains("16000");
        assertThat(context.fileContexts()).hasSize(5);

        // First four files should have truncated patches (within budget)
        for (int i = 0; i < 4; i++) {
            FileContext fc = context.fileContexts().get(i);
            assertThat(fc.patchTruncated()).isTrue();
            assertThat(fc.patchExcerpt().length()).isEqualTo(DiffContextBuilder.MAX_FILE_PATCH_EXCERPT);
        }

        // Fifth file should have placeholder since budget is exhausted
        FileContext fc5 = context.fileContexts().get(4);
        assertThat(fc5.patchTruncated()).isTrue();
        assertThat(fc5.patchExcerpt()).isEqualTo(DiffContextBuilder.NO_PATCH_PLACEHOLDER);
    }

    @Test
    void fastModePrioritizesHighRiskPatchWhenTotalBudgetIsLimited() {
        String lowRiskPatch = "+".repeat(DiffContextBuilder.MAX_FILE_PATCH_EXCERPT);
        String highRiskPatch = ("+@GetMapping(\"/admin\")\n"
                + "+return securityService.authorize(token);\n")
                .repeat(80);
        BuildDiffContextRequest request = new BuildDiffContextRequest(
                "o", "r", 8, "Risk Ordered PR",
                List.of(
                        new ChangedFile("docs/guide.md", "modified", 1, 0, 1, lowRiskPatch),
                        new ChangedFile("src/test/java/FooTest.java", "modified", 1, 0, 1, lowRiskPatch),
                        new ChangedFile("assets/logo.png", "modified", 1, 0, 1, lowRiskPatch),
                        new ChangedFile("README.md", "modified", 1, 0, 1, lowRiskPatch),
                        new ChangedFile("src/main/java/app/UserController.java", "modified", 20, 2, 22, highRiskPatch)
                ),
                AnalysisMode.FAST
        );

        DiffReviewContext context = builder.build(request);

        assertThat(context.truncated()).isTrue();
        assertThat(context.truncationReason()).contains("16000");
        assertThat(context.fileContexts()).extracting(FileContext::filename)
                .containsExactly(
                        "docs/guide.md",
                        "src/test/java/FooTest.java",
                        "assets/logo.png",
                        "README.md",
                        "src/main/java/app/UserController.java"
                );
        assertThat(context.fileContexts().get(3).patchExcerpt())
                .isEqualTo(DiffContextBuilder.NO_PATCH_PLACEHOLDER);
        assertThat(context.fileContexts().get(4).patchExcerpt())
                .contains("securityService.authorize");
        assertThat(context.fileContexts().get(4).patchTruncated()).isTrue();
    }

    @Test
    void deepModeUsesWiderContextBudgetThanFastMode() {
        List<ChangedFile> files = List.of(
                new ChangedFile("File1.java", "modified", 10, 0, 10, "a".repeat(5000)),
                new ChangedFile("File2.java", "modified", 10, 0, 10, "b".repeat(5000)),
                new ChangedFile("File3.java", "modified", 10, 0, 10, "c".repeat(5000)),
                new ChangedFile("File4.java", "modified", 10, 0, 10, "d".repeat(5000)),
                new ChangedFile("File5.java", "modified", 10, 0, 10, "e".repeat(5000))
        );

        DiffReviewContext fastContext = builder.build(new BuildDiffContextRequest(
                "o", "r", 9, "Budget PR", files, AnalysisMode.FAST));
        DiffReviewContext deepContext = builder.build(new BuildDiffContextRequest(
                "o", "r", 9, "Budget PR", files, AnalysisMode.DEEP));

        assertThat(fastContext.truncated()).isTrue();
        assertThat(fastContext.fileContexts().get(0).patchExcerpt())
                .hasSize(DiffContextBuilder.MAX_FILE_PATCH_EXCERPT);
        assertThat(deepContext.truncated()).isFalse();
        assertThat(deepContext.analysisMode()).isEqualTo(AnalysisMode.DEEP);
        assertThat(deepContext.contextStrategy()).contains("DEEP").contains("8000").contains("48000");
        assertThat(deepContext.fileContexts())
                .allSatisfy(fileContext -> assertThat(fileContext.patchExcerpt()).hasSize(5000));
    }

    @Test
    void nullPatchUsesPlaceholder() {
        BuildDiffContextRequest request = new BuildDiffContextRequest(
                "o", "r", 4, "Binary File PR",
                List.of(
                        new ChangedFile("image.png", "modified", 0, 0, 0, null)
                )
        );

        DiffReviewContext context = builder.build(request);

        assertThat(context.fileContexts()).hasSize(1);
        FileContext fc = context.fileContexts().get(0);
        assertThat(fc.patchTruncated()).isFalse();
        assertThat(fc.patchExcerpt()).isEqualTo(DiffContextBuilder.NO_PATCH_PLACEHOLDER);
        assertThat(fc.changes()).isZero();
    }

    @Test
    void blankPatchUsesPlaceholder() {
        BuildDiffContextRequest request = new BuildDiffContextRequest(
                "o", "r", 5, "Empty Patch PR",
                List.of(
                        new ChangedFile("README.md", "modified", 1, 1, 2, "   ")
                )
        );

        DiffReviewContext context = builder.build(request);

        FileContext fc = context.fileContexts().get(0);
        assertThat(fc.patchTruncated()).isFalse();
        assertThat(fc.patchExcerpt()).isEqualTo(DiffContextBuilder.NO_PATCH_PLACEHOLDER);
    }

    @Test
    void preservesFileOrder() {
        List<ChangedFile> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            files.add(new ChangedFile("File" + i + ".java", "modified", i, 0, i, "@@ patch " + i + " @@"));
        }

        BuildDiffContextRequest request = new BuildDiffContextRequest("o", "r", 6, "Ordered PR", files);
        DiffReviewContext context = builder.build(request);

        assertThat(context.fileContexts()).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(context.fileContexts().get(i).filename()).isEqualTo("File" + (i + 1) + ".java");
        }
    }

    @Test
    void mixedNullAndValidPatches() {
        String normalPatch = "@@ -1 +1 @@ normal";
        BuildDiffContextRequest request = new BuildDiffContextRequest(
                "o", "r", 7, "Mixed PR",
                List.of(
                        new ChangedFile("normal.java", "modified", 5, 2, 7, normalPatch),
                        new ChangedFile("binary.o", "modified", 0, 0, 0, null),
                        new ChangedFile("other.java", "added", 3, 0, 3, "@@ -0 +1 @@ ok")
                )
        );

        DiffReviewContext context = builder.build(request);

        assertThat(context.truncated()).isFalse();
        assertThat(context.fileContexts()).hasSize(3);
        assertThat(context.fileContexts().get(0).patchExcerpt()).isEqualTo(normalPatch);
        assertThat(context.fileContexts().get(1).patchExcerpt()).isEqualTo(DiffContextBuilder.NO_PATCH_PLACEHOLDER);
        assertThat(context.fileContexts().get(2).patchExcerpt()).isEqualTo("@@ -0 +1 @@ ok");
    }
}
