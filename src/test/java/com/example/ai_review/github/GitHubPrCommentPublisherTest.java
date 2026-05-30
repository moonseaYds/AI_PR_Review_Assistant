package com.example.ai_review.github;

import com.example.ai_review.common.GitHubApiException;
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

class GitHubPrCommentPublisherTest {

    private MockRestServiceServer server;
    private GitHubPrCommentPublisher publisher;
    private RestClient.Builder builder;

    void setUpWithToken(String token) {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        publisher = new GitHubPrCommentPublisher(builder, token);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server = null;
        }
    }

    @Test
    void sendsAuthorizationHeaderWhenTokenPresent() {
        setUpWithToken("test-token");

        server.expect(requestTo(
                        "https://api.github.com/repos/owner/repo/issues/1/comments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("User-Agent", "ai-pr-review-assistant"))
                .andExpect(jsonPath("$.body").value("## Review\n\ntest body"))
                .andRespond(withSuccess("""
                        {"html_url": "https://github.com/owner/repo/pull/1#issuecomment-123"}
                        """, MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1,
                "https://github.com/owner/repo/pull/1");
        PublishPullRequestCommentResponse response = publisher.publish(ref, "## Review\n\ntest body");

        assertThat(response.commentUrl()).isEqualTo(
                "https://github.com/owner/repo/pull/1#issuecomment-123");
        assertThat(response.owner()).isEqualTo("owner");
        assertThat(response.repo()).isEqualTo("repo");
        assertThat(response.pullNumber()).isEqualTo(1);
    }

    @Test
    void mapsHtmlUrlToCommentUrl() {
        setUpWithToken("token");

        server.expect(requestTo(
                        "https://api.github.com/repos/o/r/issues/2/comments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"html_url": "https://github.com/o/r/pull/2#issuecomment-456"}
                        """, MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 2,
                "https://github.com/o/r/pull/2");
        PublishPullRequestCommentResponse response = publisher.publish(ref, "body");

        assertThat(response.commentUrl()).isEqualTo(
                "https://github.com/o/r/pull/2#issuecomment-456");
    }

    @Test
    void throwsWhenTokenEmpty() {
        setUpWithToken("");

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 1,
                "https://github.com/o/r/pull/1");

        assertThatThrownBy(() -> publisher.publish(ref, "body"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GITHUB_TOKEN");
    }

    @Test
    void throwsOn401() {
        setUpWithToken("bad-token");

        server.expect(requestTo(
                        "https://api.github.com/repos/o/r/issues/5/comments"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body("{\"message\":\"Bad credentials\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 5,
                "https://github.com/o/r/pull/5");

        assertThatThrownBy(() -> publisher.publish(ref, "body"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("认证失败或权限不足");
    }

    @Test
    void throwsOn403() {
        setUpWithToken("no-permission-token");

        server.expect(requestTo(
                        "https://api.github.com/repos/o/r/issues/3/comments"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body("{\"message\":\"Forbidden\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 3,
                "https://github.com/o/r/pull/3");

        assertThatThrownBy(() -> publisher.publish(ref, "body"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("认证失败或权限不足");
    }

    @Test
    void throwsOn404() {
        setUpWithToken("token");

        server.expect(requestTo(
                        "https://api.github.com/repos/o/r/issues/999/comments"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .body("{\"message\":\"Not Found\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 999,
                "https://github.com/o/r/pull/999");

        assertThatThrownBy(() -> publisher.publish(ref, "body"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("PR 或仓库不存在");
    }

    @Test
    void throwsOn5xx() {
        setUpWithToken("token");

        server.expect(requestTo(
                        "https://api.github.com/repos/o/r/issues/7/comments"))
                .andRespond(withServerError());

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 7,
                "https://github.com/o/r/pull/7");

        assertThatThrownBy(() -> publisher.publish(ref, "body"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub 服务暂时不可用");
    }
}
