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
import static org.mockito.ArgumentMatchers.eq;
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
                .andExpect(jsonPath("$.code").value("INVALID_PR_URL"))
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

        when(gitHubPrFetcher.fetch(any(), any())).thenReturn(mockResult);

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
                .andExpect(jsonPath("$.code").value("INVALID_PR_URL"))
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

    @Test
    void buildDiffContextReturnsUntruncatedForSmallDiff() throws Exception {
        mockMvc.perform(post("/api/reviews/build-diff-context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "o",
                                  "repo": "r",
                                  "pullNumber": 1,
                                  "title": "Small PR",
                                  "changedFiles": [
                                    {
                                      "filename": "A.java",
                                      "status": "modified",
                                      "additions": 3,
                                      "deletions": 1,
                                      "changes": 4,
                                      "patch": "@@ -1 +1 @@ short"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("o"))
                .andExpect(jsonPath("$.repo").value("r"))
                .andExpect(jsonPath("$.pullNumber").value(1))
                .andExpect(jsonPath("$.title").value("Small PR"))
                .andExpect(jsonPath("$.totalFiles").value(1))
                .andExpect(jsonPath("$.totalAdditions").value(3))
                .andExpect(jsonPath("$.totalDeletions").value(1))
                .andExpect(jsonPath("$.totalChanges").value(4))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.truncationReason").doesNotExist())
                .andExpect(jsonPath("$.fileContexts[0].filename").value("A.java"))
                .andExpect(jsonPath("$.fileContexts[0].patchTruncated").value(false));
    }

    @Test
    void buildDiffContextTruncatesLongPatch() throws Exception {
        String longPatch = "x".repeat(5000);
        mockMvc.perform(post("/api/reviews/build-diff-context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "o",
                                  "repo": "r",
                                  "pullNumber": 2,
                                  "title": "Large PR",
                                  "changedFiles": [
                                    {
                                      "filename": "Big.java",
                                      "status": "modified",
                                      "additions": 100,
                                      "deletions": 50,
                                      "changes": 150,
                                      "patch": "%s"
                                    }
                                  ]
                                }
                                """.formatted(longPatch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truncated").value(true))
                .andExpect(jsonPath("$.truncationReason").isNotEmpty())
                .andExpect(jsonPath("$.fileContexts[0].patchTruncated").value(true));
    }

    @Test
    void buildDiffContextReturns400ForEmptyChangedFiles() throws Exception {
        mockMvc.perform(post("/api/reviews/build-diff-context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "o",
                                  "repo": "r",
                                  "pullNumber": 1,
                                  "title": "Empty PR",
                                  "changedFiles": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void buildDiffContextReturns400ForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/reviews/build-diff-context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "",
                                  "repo": "r",
                                  "pullNumber": 0,
                                  "title": "",
                                  "changedFiles": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
