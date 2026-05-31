package com.example.ai_review.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StaticPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootReturnsForwardToIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void indexHtmlContainsTitle() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AI PR Review Assistant")));
    }

    @Test
    void apiJsIsAccessible() throws Exception {
        mockMvc.perform(get("/js/api.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("javascript")));
    }

    @Test
    void renderJsIsAccessible() throws Exception {
        mockMvc.perform(get("/js/render.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("javascript")));
    }

    @Test
    void renderJsHasCorrectBranchLabels() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/render.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("源分支"),
                "render.js should contain 源分支 label");
        assertTrue(body.contains("data.headBranch"),
                "render.js should reference data.headBranch");
        assertTrue(body.contains("目标分支"),
                "render.js should contain 目标分支 label");
        assertTrue(body.contains("data.baseBranch"),
                "render.js should reference data.baseBranch");
    }

    @Test
    void appJsIsAccessible() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("javascript")));
    }

    @Test
    void stylesCssIsAccessible() throws Exception {
        mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("css")));
    }

    @Test
    void stylesCssHasMobileOverflowPrevention() throws Exception {
        byte[] bytes = mockMvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("min(100% - 40px, 860px)"),
                "styles.css should use explicit desktop gutter width");
        assertTrue(body.contains("calc(100% - 28px)"),
                "styles.css should use explicit mobile gutter width");
        assertTrue(body.contains("min-width: 0"),
                "styles.css should use min-width: 0 for flex children");
        assertTrue(body.contains("word-break: break-word"),
                "styles.css should use word-break for long text on mobile");
        assertTrue(body.contains(".footer p") || body.contains("footer p"),
                "styles.css should have footer p rule with max-width");
    }

    @Test
    void apiJsHasPublishComment() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/api.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("publishComment"),
                "api.js should contain publishComment function");
        assertTrue(body.contains("publish-comment"),
                "api.js should reference publish-comment endpoint");
    }

    @Test
    void renderJsHasPublishForm() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/render.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("publishForm"),
                "render.js should contain publishForm function");
        assertTrue(body.contains("codeSnippet"),
                "render.js should handle codeSnippet display");
        assertTrue(body.contains("exampleFix"),
                "render.js should handle exampleFix display");
        assertTrue(body.contains("retryable"),
                "render.js should display retryable status");
    }

    @Test
    void renderJsPublishErrorHasStructuredDisplay() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/render.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("publishError"),
                "render.js should have publishError function");
        assertTrue(body.contains("retryable") || body.contains("可重试"),
                "render.js publishError should handle retryable");
        assertTrue(body.contains("suggestion") || body.contains("建议"),
                "render.js publishError should handle suggestion");
    }

    @Test
    void appJsPassesFullErrToPublishError() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("publishError(err)"),
                "app.js should pass full err to publishError");
    }

    @Test
    void appJsHasLastPrUrl() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("lastPrUrl"),
                "app.js should store lastPrUrl for publish flow");
        assertTrue(body.contains("publish-btn"),
                "app.js should handle publish button click");
    }

    @Test
    void indexHtmlHasLocalDiffTextarea() throws Exception {
        byte[] bytes = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("本地 Diff"),
                "index.html should contain local diff mode label");
        assertTrue(body.contains("diff-text"),
                "index.html should contain diff-text textarea id");
        assertTrue(body.contains("analyze-diff"),
                "index.html footer should reference analyze-diff endpoint");
    }

    @Test
    void indexHtmlHasReviewContextModeSelector() throws Exception {
        byte[] bytes = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("analysis-mode"),
                "index.html should contain analysis-mode selector");
        assertTrue(body.contains("FAST"),
                "index.html should expose FAST mode");
        assertTrue(body.contains("DEEP"),
                "index.html should expose DEEP mode");
        assertTrue(body.contains("Review Context"),
                "index.html should explain Review Context strategy");
    }

    @Test
    void apiJsHasAnalyzeDiff() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/api.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("analyzeDiff"),
                "api.js should contain analyzeDiff function");
        assertTrue(body.contains("analyze-diff"),
                "api.js should reference analyze-diff endpoint");
        assertTrue(body.contains("analysisMode"),
                "api.js should send analysisMode in request body");
    }

    @Test
    void appJsHasLocalDiffMode() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("analyzeLocalDiff"),
                "app.js should contain analyzeLocalDiff function");
        assertTrue(body.contains("local-diff"),
                "app.js should handle local-diff mode");
        assertTrue(body.contains("diff-text") && body.contains("addEventListener"),
                "app.js should listen to diff-text input events");
        assertTrue(body.contains("getAnalysisMode"),
                "app.js should read selected Review Context mode");
        assertTrue(body.contains("analysis-mode"),
                "app.js should reference analysis-mode selector");
    }

    @Test
    void renderJsDisplaysContextModeAndStrategy() throws Exception {
        byte[] bytes = mockMvc.perform(get("/js/render.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(body.contains("Context 模式"),
                "render.js should display context mode");
        assertTrue(body.contains("contextStrategy"),
                "render.js should display context strategy");
        assertTrue(body.contains("batchReview"),
                "render.js should display batch review state");
        assertTrue(body.contains("batchStrategy"),
                "render.js should display batch review strategy");
        assertTrue(body.contains("Diff 已截断"),
                "render.js should still display truncation status");
    }
}
