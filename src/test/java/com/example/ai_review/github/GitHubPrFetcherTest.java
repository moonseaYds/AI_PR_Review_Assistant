package com.example.ai_review.github;

import com.example.ai_review.common.GitHubApiException;
import com.example.ai_review.common.RuntimeCredentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GitHubPrFetcherTest {

    private MockRestServiceServer server;
    private GitHubPrFetcher fetcher;
    private RestClient.Builder builder;

    void setUpWithToken(String token) {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        fetcher = new GitHubPrFetcher(builder, token);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server = null;
        }
    }

    @Test
    void sendsAuthorizationHeaderWhenTokenPresent() {
        setUpWithToken("test-token-for-unit-test");

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/pulls/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token-for-unit-test"))
                .andExpect(header("Accept", "application/vnd.github.v3+json"))
                .andExpect(header("User-Agent", "ai-pr-review-assistant"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/pulls/1/files"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token-for-unit-test"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1,
                "https://github.com/owner/repo/pull/1");

        // Should not throw — but the response has null fields, so it will proceed and return
        // We just care about the header assertions
        fetcher.fetch(ref);
        server.verify();
    }

    @Test
    void doesNotSendAuthorizationHeaderWhenTokenEmpty() {
        setUpWithToken("");

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/pulls/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    // Authorization header must not be present
                    assertThat(request.getHeaders().containsKey("Authorization")).isFalse();
                })
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/pulls/1/files"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    assertThat(request.getHeaders().containsKey("Authorization")).isFalse();
                })
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1,
                "https://github.com/owner/repo/pull/1");

        fetcher.fetch(ref);
        server.verify();
    }

    @Test
    void runtimeTokenOverridesEmptyEnvironmentToken() {
        setUpWithToken("");

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/pulls/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer runtime-token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/pulls/1/files"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer runtime-token"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1,
                "https://github.com/owner/repo/pull/1");

        fetcher.fetch(ref, new RuntimeCredentials(null, " runtime-token "));
        server.verify();
    }

    @Test
    void throwsClearErrorFor404() {
        setUpWithToken("");

        server.expect(requestTo(
                        "https://api.github.com/repos/nonexistent/repo/pulls/999"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .body("{\"message\":\"Not Found\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("nonexistent", "repo", 999,
                "https://github.com/nonexistent/repo/pull/999");

        assertThatThrownBy(() -> fetcher.fetch(ref))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub PR 不存在")
                .hasMessageContaining("nonexistent/repo#999");
    }

    @Test
    void throwsClearErrorForRateLimit() {
        setUpWithToken("");

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/pulls/1"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body("{\"message\":\"API rate limit exceeded\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1,
                "https://github.com/owner/repo/pull/1");

        assertThatThrownBy(() -> fetcher.fetch(ref))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API 限流")
                .hasMessageContaining("GITHUB_TOKEN");
    }

    @Test
    void throwsClearErrorForServerError() {
        setUpWithToken("");

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/pulls/1"))
                .andRespond(withServerError()
                        .body("{\"message\":\"Internal Server Error\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1,
                "https://github.com/owner/repo/pull/1");

        assertThatThrownBy(() -> fetcher.fetch(ref))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub 服务暂时不可用");
    }

    @Test
    void fetchesPrWithValidResponse() {
        setUpWithToken("");

        String prJson = """
                {
                  "title": "Test PR",
                  "user": { "login": "testuser" },
                  "state": "open",
                  "base": { "ref": "main" },
                  "head": { "ref": "feature/test" }
                }
                """;

        String filesJson = """
                [
                  { "filename": "App.java", "status": "modified", "additions": 5, "deletions": 2, "changes": 7, "patch": "@@ -1 +1 @@" }
                ]
                """;

        server.expect(requestTo(
                        "https://api.github.com/repos/o/r/pulls/10"))
                .andRespond(withSuccess(prJson, MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://api.github.com/repos/o/r/pulls/10/files"))
                .andRespond(withSuccess(filesJson, MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 10,
                "https://github.com/o/r/pull/10");

        PrFetchResult result = fetcher.fetch(ref);

        assertThat(result.title()).isEqualTo("Test PR");
        assertThat(result.author()).isEqualTo("testuser");
        assertThat(result.state()).isEqualTo("open");
        assertThat(result.baseBranch()).isEqualTo("main");
        assertThat(result.headBranch()).isEqualTo("feature/test");
        assertThat(result.changedFiles()).hasSize(1);
        assertThat(result.changedFiles().get(0).filename()).isEqualTo("App.java");

        server.verify();
    }
}
