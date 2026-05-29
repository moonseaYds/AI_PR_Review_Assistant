package com.example.ai_review.github;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestParseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void parsesPullRequestUrl() throws Exception {
        mockMvc.perform(post("/api/reviews/parse-pr-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://github.com/spring-projects/spring-boot/pull/12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("spring-projects"))
                .andExpect(jsonPath("$.repo").value("spring-boot"))
                .andExpect(jsonPath("$.pullNumber").value(12345))
                .andExpect(jsonPath("$.normalizedUrl").value("https://github.com/spring-projects/spring-boot/pull/12345"));
    }

    @Test
    void returnsBadRequestForInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/reviews/parse-pr-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": "https://example.com/owner/repo/pull/1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("github.com")));
    }
}
