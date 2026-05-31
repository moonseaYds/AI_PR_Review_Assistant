package com.example.ai_review.github;

import com.example.ai_review.common.ErrorCode;
import com.example.ai_review.common.GitHubApiException;
import com.example.ai_review.common.RuntimeCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
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
                })
                .build();
    }

    public PrFetchResult fetch(GitHubPullRequestRef ref) {
        return fetch(ref, RuntimeCredentials.empty());
    }

    public PrFetchResult fetch(GitHubPullRequestRef ref, RuntimeCredentials credentials) {
        String effectiveToken = effectiveToken(credentials);
        GitHubPrResponse prResponse = fetchPr(ref, effectiveToken);
        List<GitHubPrFileResponse> fileResponses = fetchFiles(ref, effectiveToken);

        return mapToResult(ref, prResponse, fileResponses);
    }

    private GitHubPrResponse fetchPr(GitHubPullRequestRef ref, String effectiveToken) {
        String url = GITHUB_API_BASE + "/repos/%s/%s/pulls/%d"
                .formatted(ref.owner(), ref.repo(), ref.pullNumber());

        return restClient.get()
                .uri(url)
                .headers(headers -> applyAuthorization(headers, effectiveToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    byte[] body = response.getBody().readAllBytes();
                    String bodyText = new String(body);
                    if (response.getStatusCode().value() == 404) {
                        throw new GitHubApiException(ErrorCode.GITHUB_PR_NOT_FOUND,
                                "GitHub PR 不存在：%s/%s#%d，请检查 owner、仓库名或 PR 编号是否正确"
                                        .formatted(ref.owner(), ref.repo(), ref.pullNumber()),
                                "确认 PR 链接正确；私有仓库需配置有读权限的 GITHUB_TOKEN", false);
                    }
                    if (response.getStatusCode().value() == 403 && bodyText.contains("rate limit")) {
                        throw new GitHubApiException(ErrorCode.GITHUB_RATE_LIMITED,
                                "GitHub API 限流：匿名访问限制较低，请设置 GITHUB_TOKEN 环境变量提高限额",
                                "设置 GITHUB_TOKEN 后重试；或稍后重试", true);
                    }
                    throw new GitHubApiException(ErrorCode.GITHUB_UPSTREAM_ERROR,
                            "GitHub API 返回错误 (" + response.getStatusCode().value() + ")：" + bodyText,
                            "检查请求参数和 token 权限", false);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new GitHubApiException(ErrorCode.GITHUB_UPSTREAM_ERROR,
                            "GitHub 服务暂时不可用，请稍后重试",
                            "稍后重试；如果只是提交前自查，可切换到本地 Diff Review", true);
                })
                .body(GitHubPrResponse.class);
    }

    private List<GitHubPrFileResponse> fetchFiles(GitHubPullRequestRef ref, String effectiveToken) {
        String url = GITHUB_API_BASE + "/repos/%s/%s/pulls/%d/files"
                .formatted(ref.owner(), ref.repo(), ref.pullNumber());

        GitHubPrFileResponse[] files = restClient.get()
                .uri(url)
                .headers(headers -> applyAuthorization(headers, effectiveToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    byte[] body = response.getBody().readAllBytes();
                    String bodyText = new String(body);
                    int status = response.getStatusCode().value();
                    if (status == 404) {
                        throw new GitHubApiException(ErrorCode.GITHUB_PR_NOT_FOUND,
                                "获取 PR 变更文件列表失败：仓库或 PR 不存在",
                                "确认 PR 链接正确；私有仓库需配置有读权限的 GITHUB_TOKEN", false);
                    }
                    if (status == 403 && bodyText.contains("rate limit")) {
                        throw new GitHubApiException(ErrorCode.GITHUB_RATE_LIMITED,
                                "GitHub API 限流：无法获取变更文件列表",
                                "设置 GITHUB_TOKEN 后重试；或稍后重试", true);
                    }
                    if (status == 401 || status == 403) {
                        throw new GitHubApiException(ErrorCode.GITHUB_AUTH_FAILED,
                                "获取 PR 变更文件列表失败：认证失败或权限不足",
                                "检查 GITHUB_TOKEN 是否有效且有目标仓库读权限", false);
                    }
                    throw new GitHubApiException(ErrorCode.GITHUB_UPSTREAM_ERROR,
                            "获取 PR 变更文件列表失败，GitHub API 返回 " + status,
                            "检查请求参数和 token 权限", false);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new GitHubApiException(ErrorCode.GITHUB_UPSTREAM_ERROR,
                            "GitHub 服务暂时不可用，请稍后重试",
                            "稍后重试；如果只是提交前自查，可切换到本地 Diff Review", true);
                })
                .body(GitHubPrFileResponse[].class);

        return files != null ? Arrays.asList(files) : List.of();
    }

    private String effectiveToken(RuntimeCredentials credentials) {
        String runtimeToken = credentials != null ? credentials.normalizedGitHubToken() : "";
        return !runtimeToken.isEmpty() ? runtimeToken : token;
    }

    private void applyAuthorization(HttpHeaders headers, String effectiveToken) {
        if (!effectiveToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + effectiveToken);
        }
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
