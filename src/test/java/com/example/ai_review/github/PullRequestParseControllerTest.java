package com.example.ai_review.github;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestParseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubPrFetcher gitHubPrFetcher;

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

    @Test
    void fetchPrReturnsPrDetails() throws Exception {
        PrFetchResult mockResult = new PrFetchResult(
                "spring-projects", "spring-boot", 12345,
                "Fix login bug", "octocat", "open",
                "main", "feature/login-fix",
                List.of(new ChangedFile("App.java", "modified", 5, 2, 7, "@@ -1 +1 @@"))
        );

        when(gitHubPrFetcher.fetch(any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/reviews/fetch-pr")
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
                .andExpect(jsonPath("$.title").value("Fix login bug"))
                .andExpect(jsonPath("$.author").value("octocat"))
                .andExpect(jsonPath("$.state").value("open"))
                .andExpect(jsonPath("$.baseBranch").value("main"))
                .andExpect(jsonPath("$.headBranch").value("feature/login-fix"))
                .andExpect(jsonPath("$.changedFiles[0].filename").value("App.java"))
                .andExpect(jsonPath("$.changedFiles[0].status").value("modified"))
                .andExpect(jsonPath("$.changedFiles[0].additions").value(5))
                .andExpect(jsonPath("$.changedFiles[0].deletions").value(2));
    }

    @Test
    void fetchPrReturnsBadRequestForInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/reviews/fetch-pr")
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

    @Test
    void fetchPrReturnsBadRequestForEmptyUrl() throws Exception {
        mockMvc.perform(post("/api/reviews/fetch-pr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prUrl": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
