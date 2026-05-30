package com.example.ai_review.github;

import com.example.ai_review.common.ErrorCode;
import com.example.ai_review.common.GitHubApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class GitHubPrCommentPublisher {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final RestClient restClient;
    private final String token;

    public GitHubPrCommentPublisher(RestClient.Builder restClientBuilder,
                                     @Value("${github.token:}") String token) {
        this.token = (token != null) ? token.strip() : "";

        this.restClient = restClientBuilder
                .defaultHeaders(headers -> {
                    headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github.v3+json")));
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("User-Agent", "ai-pr-review-assistant");
                    if (!this.token.isEmpty()) {
                        headers.set("Authorization", "Bearer " + this.token);
                    }
                })
                .build();
    }

    public PublishPullRequestCommentResponse publish(GitHubPullRequestRef ref, String body) {
        if (token.isEmpty()) {
            throw new GitHubApiException(ErrorCode.GITHUB_TOKEN_REQUIRED,
                    "发布 PR 评论需要配置 GITHUB_TOKEN，请设置一个有目标仓库评论权限的 GitHub Token",
                    "在环境变量中设置 GITHUB_TOKEN 后重试", false);
        }

        String url = GITHUB_API_BASE + "/repos/%s/%s/issues/%d/comments"
                .formatted(ref.owner(), ref.repo(), ref.pullNumber());

        GitHubCommentCreateRequest requestBody = new GitHubCommentCreateRequest(body);

        try {
            GitHubCommentResponse commentResponse = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        byte[] respBody = response.getBody().readAllBytes();
                        String bodyText = new String(respBody);
                        int status = response.getStatusCode().value();
                        if (status == 401 || status == 403) {
                            throw new GitHubApiException(ErrorCode.GITHUB_AUTH_FAILED,
                                    "GitHub API 认证失败或权限不足，请检查 GITHUB_TOKEN 是否有目标仓库的评论权限",
                                    "检查 token 是否过期、是否有目标仓库的读写或评论权限", false);
                        }
                        if (status == 404) {
                            throw new GitHubApiException(ErrorCode.GITHUB_PR_NOT_FOUND,
                                    "PR 或仓库不存在，或 token 无权访问该仓库：" +
                                            ref.owner() + "/" + ref.repo() + "#" + ref.pullNumber(),
                                    "确认 PR 链接和仓库名称正确", false);
                        }
                        throw new GitHubApiException(ErrorCode.GITHUB_UPSTREAM_ERROR,
                                "GitHub API 返回错误 (" + status + ")："
                                        + (bodyText.length() > 300 ? bodyText.substring(0, 300) : bodyText),
                                "检查请求参数和 token 权限", false);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new GitHubApiException(ErrorCode.GITHUB_UPSTREAM_ERROR,
                                "GitHub 服务暂时不可用，请稍后重试",
                                "稍后重试", true);
                    })
                    .body(GitHubCommentResponse.class);

            if (commentResponse == null || commentResponse.htmlUrl() == null) {
                throw new GitHubApiException("GitHub API 返回异常：无法获取评论链接");
            }

            return new PublishPullRequestCommentResponse(
                    ref.owner(), ref.repo(), ref.pullNumber(), commentResponse.htmlUrl());

        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException(ErrorCode.GITHUB_NETWORK_ERROR,
                    "发布 PR 评论时发生网络错误，请检查网络连接",
                    "检查网络后重试", true, e);
        }
    }

    record GitHubCommentCreateRequest(String body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubCommentResponse(
            @JsonProperty("html_url") String htmlUrl) {
    }
}
