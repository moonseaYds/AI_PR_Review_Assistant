package com.example.ai_review.github;

import com.example.ai_review.common.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubPullRequestUrlParserTest {

    private final GitHubPullRequestUrlParser parser = new GitHubPullRequestUrlParser();

    @Test
    void parsesStandardPullRequestUrl() {
        GitHubPullRequestRef ref = parser.parse("https://github.com/spring-projects/spring-boot/pull/12345");

        assertThat(ref.owner()).isEqualTo("spring-projects");
        assertThat(ref.repo()).isEqualTo("spring-boot");
        assertThat(ref.pullNumber()).isEqualTo(12345);
        assertThat(ref.normalizedUrl()).isEqualTo("https://github.com/spring-projects/spring-boot/pull/12345");
    }

    @Test
    void normalizesTrailingSlashAndWwwHost() {
        GitHubPullRequestRef ref = parser.parse("https://www.github.com/moonseaYds/AI_PR_Review_Assistant/pull/1/");

        assertThat(ref.normalizedUrl()).isEqualTo("https://github.com/moonseaYds/AI_PR_Review_Assistant/pull/1");
    }

    @Test
    void rejectsNonGithubHost() {
        assertThatThrownBy(() -> parser.parse("https://example.com/owner/repo/pull/1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("github.com");
    }

    @Test
    void rejectsNonPullRequestPath() {
        assertThatThrownBy(() -> parser.parse("https://github.com/owner/repo/issues/1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("标准 GitHub PR 链接");
    }

    @Test
    void rejectsZeroPullNumber() {
        assertThatThrownBy(() -> parser.parse("https://github.com/owner/repo/pull/0"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("大于 0");
    }
}
