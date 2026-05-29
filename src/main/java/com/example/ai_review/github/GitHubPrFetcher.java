package com.example.ai_review.github;

import com.example.ai_review.common.GitHubApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Service
public class GitHubPrFetcher {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final RestClient restClient;
    private final String token;

    public GitHubPrFetcher(RestClient.Builder restClientBuilder,
                            @Value("${github.token:}") String token) {
        this.token = (token != null) ? token.strip() : "";

        this.restClient = restClientBuilder
                .defaultHeaders(headers -> {
                    headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github.v3+json")));
                    headers.set("User-Agent", "ai-pr-review-assistant");
                    if (!this.token.isEmpty()) {
                        headers.set("Authorization", "Bearer " + this.token);
                    }
                })
                .build();
    }

    public PrFetchResult fetch(GitHubPullRequestRef ref) {
        GitHubPrResponse prResponse = fetchPr(ref);
        List<GitHubPrFileResponse> fileResponses = fetchFiles(ref);

        return mapToResult(ref, prResponse, fileResponses);
    }

    private GitHubPrResponse fetchPr(GitHubPullRequestRef ref) {
        String url = GITHUB_API_BASE + "/repos/%s/%s/pulls/%d"
                .formatted(ref.owner(), ref.repo(), ref.pullNumber());

        return restClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    byte[] body = response.getBody().readAllBytes();
                    String bodyText = new String(body);
                    if (response.getStatusCode().value() == 404) {
                        throw new GitHubApiException(
                                "GitHub PR 不存在：%s/%s#%d，请检查 owner、仓库名或 PR 编号是否正确"
                                        .formatted(ref.owner(), ref.repo(), ref.pullNumber()));
                    }
                    if (response.getStatusCode().value() == 403 && bodyText.contains("rate limit")) {
                        throw new GitHubApiException(
                                "GitHub API 限流：匿名访问限制较低，请设置 GITHUB_TOKEN 环境变量提高限额");
                    }
                    throw new GitHubApiException(
                            "GitHub API 返回错误 (" + response.getStatusCode().value() + ")：" + bodyText);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new GitHubApiException("GitHub 服务暂时不可用，请稍后重试");
                })
                .body(GitHubPrResponse.class);
    }

    private List<GitHubPrFileResponse> fetchFiles(GitHubPullRequestRef ref) {
        String url = GITHUB_API_BASE + "/repos/%s/%s/pulls/%d/files"
                .formatted(ref.owner(), ref.repo(), ref.pullNumber());

        GitHubPrFileResponse[] files = restClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new GitHubApiException(
                            "获取 PR 变更文件列表失败，GitHub API 返回 " + response.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new GitHubApiException("GitHub 服务暂时不可用，请稍后重试");
                })
                .body(GitHubPrFileResponse[].class);

        return files != null ? Arrays.asList(files) : List.of();
    }

    PrFetchResult mapToResult(GitHubPullRequestRef ref, GitHubPrResponse pr,
                                       List<GitHubPrFileResponse> files) {
        List<ChangedFile> changedFiles = files.stream()
                .map(f -> new ChangedFile(
                        f.filename(),
                        f.status(),
                        f.additions(),
                        f.deletions(),
                        f.changes(),
                        f.patch()))
                .toList();

        return new PrFetchResult(
                ref.owner(),
                ref.repo(),
                ref.pullNumber(),
                pr.title(),
                pr.user() != null ? pr.user().login() : "unknown",
                pr.state(),
                pr.base() != null ? pr.base().ref() : "unknown",
                pr.head() != null ? pr.head().ref() : "unknown",
                changedFiles
        );
    }
}
