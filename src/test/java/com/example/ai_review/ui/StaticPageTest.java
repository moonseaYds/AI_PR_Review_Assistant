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
}
