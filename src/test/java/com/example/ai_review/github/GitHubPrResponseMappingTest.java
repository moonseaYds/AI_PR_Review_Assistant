package com.example.ai_review.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubPrResponseMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsPrDetailsFromGitHubJson() throws Exception {
        String json = """
                {
                  "title": "Fix login bug",
                  "user": { "login": "octocat" },
                  "state": "open",
                  "base": { "ref": "main" },
                  "head": { "ref": "feature/login-fix" }
                }
                """;

        GitHubPrResponse response = objectMapper.readValue(json, GitHubPrResponse.class);

        assertThat(response.title()).isEqualTo("Fix login bug");
        assertThat(response.user().login()).isEqualTo("octocat");
        assertThat(response.state()).isEqualTo("open");
        assertThat(response.base().ref()).isEqualTo("main");
        assertThat(response.head().ref()).isEqualTo("feature/login-fix");
    }

    @Test
    void mapsPrDetailsWithNullUser() throws Exception {
        String json = """
                {
                  "title": "Some PR",
                  "user": null,
                  "state": "closed",
                  "base": { "ref": "develop" },
                  "head": { "ref": "fix/something" }
                }
                """;

        GitHubPrResponse response = objectMapper.readValue(json, GitHubPrResponse.class);

        assertThat(response.user()).isNull();
        assertThat(response.state()).isEqualTo("closed");
    }

    @Test
    void mapsFileDetailsFromGitHubJson() throws Exception {
        String json = """
                {
                  "filename": "src/main/java/App.java",
                  "status": "modified",
                  "additions": 10,
                  "deletions": 3,
                  "changes": 13,
                  "patch": "@@ -1,3 +1,10 @@ ..."
                }
                """;

        GitHubPrFileResponse response = objectMapper.readValue(json, GitHubPrFileResponse.class);

        assertThat(response.filename()).isEqualTo("src/main/java/App.java");
        assertThat(response.status()).isEqualTo("modified");
        assertThat(response.additions()).isEqualTo(10);
        assertThat(response.deletions()).isEqualTo(3);
        assertThat(response.changes()).isEqualTo(13);
        assertThat(response.patch()).startsWith("@@");
    }

    @Test
    void mapsFileDetailsWithNullPatch() throws Exception {
        String json = """
                {
                  "filename": "README.md",
                  "status": "added",
                  "additions": 5,
                  "deletions": 0,
                  "changes": 5,
                  "patch": null
                }
                """;

        GitHubPrFileResponse response = objectMapper.readValue(json, GitHubPrFileResponse.class);

        assertThat(response.patch()).isNull();
        assertThat(response.status()).isEqualTo("added");
    }

    @Test
    void mapsMultipleFilesFromGitHubJson() throws Exception {
        String json = """
                [
                  { "filename": "A.java", "status": "modified", "additions": 1, "deletions": 0, "changes": 1, "patch": "@@ ... @@" },
                  { "filename": "B.java", "status": "added", "additions": 20, "deletions": 0, "changes": 20, "patch": "@@ ... @@" }
                ]
                """;

        GitHubPrFileResponse[] responses = objectMapper.readValue(json, GitHubPrFileResponse[].class);

        assertThat(responses).hasSize(2);
        assertThat(responses[0].filename()).isEqualTo("A.java");
        assertThat(responses[1].filename()).isEqualTo("B.java");
    }

    @Test
    void ignoresUnknownJsonFields() throws Exception {
        String json = """
                {
                  "title": "Test PR",
                  "user": { "login": "dev", "extra_field": "ignored" },
                  "state": "open",
                  "base": { "ref": "main", "sha": "abc123" },
                  "head": { "ref": "feature/x", "label": "dev:feature/x" },
                  "unknown_top_level": "should be ignored"
                }
                """;

        GitHubPrResponse response = objectMapper.readValue(json, GitHubPrResponse.class);

        assertThat(response.title()).isEqualTo("Test PR");
        assertThat(response.user().login()).isEqualTo("dev");
    }

    @Test
    void mapToResultHandlesNullUserGracefully() {
        GitHubPrResponse pr = new GitHubPrResponse("Title", null, "open",
                new GitHubPrResponse.Base("main"),
                new GitHubPrResponse.Head("feature/x"));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("owner", "repo", 1, "https://github.com/owner/repo/pull/1");
        List<GitHubPrFileResponse> files = List.of();

        GitHubPrFetcher fetcher = new GitHubPrFetcher(
                org.springframework.web.client.RestClient.builder(), "");
        PrFetchResult result = fetcher.mapToResult(ref, pr, files);

        assertThat(result.author()).isEqualTo("unknown");
    }

    @Test
    void mapToResultConvertsFilesCorrectly() {
        GitHubPrResponse pr = new GitHubPrResponse("Title", new GitHubPrResponse.User("dev"),
                "open", new GitHubPrResponse.Base("main"), new GitHubPrResponse.Head("feat/x"));

        GitHubPullRequestRef ref = new GitHubPullRequestRef("o", "r", 5, "https://github.com/o/r/pull/5");
        List<GitHubPrFileResponse> files = List.of(
                new GitHubPrFileResponse("A.java", "modified", 3, 1, 4, "@@ patch"),
                new GitHubPrFileResponse("B.java", "added", 10, 0, 10, null)
        );

        GitHubPrFetcher fetcher = new GitHubPrFetcher(
                org.springframework.web.client.RestClient.builder(), "");
        PrFetchResult result = fetcher.mapToResult(ref, pr, files);

        assertThat(result.owner()).isEqualTo("o");
        assertThat(result.repo()).isEqualTo("r");
        assertThat(result.pullNumber()).isEqualTo(5);
        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.author()).isEqualTo("dev");
        assertThat(result.state()).isEqualTo("open");
        assertThat(result.baseBranch()).isEqualTo("main");
        assertThat(result.headBranch()).isEqualTo("feat/x");
        assertThat(result.changedFiles()).hasSize(2);
        assertThat(result.changedFiles().get(0).filename()).isEqualTo("A.java");
        assertThat(result.changedFiles().get(0).changes()).isEqualTo(4);
        assertThat(result.changedFiles().get(1).patch()).isNull();
    }
}
