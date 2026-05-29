package com.example.ai_review.github;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubPullRequestUrlParser {

    private static final Pattern PULL_REQUEST_PATH = Pattern.compile("^/([^/]+)/([^/]+)/pull/(\\d+)/?$");

    public GitHubPullRequestRef parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("GitHub PR 链接不能为空");
        }

        URI uri = parseUri(rawUrl.trim());
        validateScheme(uri);
        validateHost(uri);

        Matcher matcher = PULL_REQUEST_PATH.matcher(uri.getPath());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("请输入标准 GitHub PR 链接，例如 https://github.com/owner/repo/pull/123");
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);
        int pullNumber = parsePullNumber(matcher.group(3));
        String normalizedUrl = "https://github.com/%s/%s/pull/%d".formatted(owner, repo, pullNumber);

        return new GitHubPullRequestRef(owner, repo, pullNumber, normalizedUrl);
    }

    private URI parseUri(String rawUrl) {
        try {
            return new URI(rawUrl);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("GitHub PR 链接格式不合法");
        }
    }

    private void validateScheme(URI uri) {
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("GitHub PR 链接必须以 http:// 或 https:// 开头");
        }
    }

    private void validateHost(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("GitHub PR 链接缺少域名");
        }

        String normalizedHost = host.toLowerCase();
        if (normalizedHost.startsWith("www.")) {
            normalizedHost = normalizedHost.substring(4);
        }

        if (!"github.com".equals(normalizedHost)) {
            throw new IllegalArgumentException("当前仅支持 github.com 的 PR 链接");
        }
    }

    private int parsePullNumber(String rawPullNumber) {
        try {
            int pullNumber = Integer.parseInt(rawPullNumber);
            if (pullNumber <= 0) {
                throw new IllegalArgumentException("GitHub PR 编号必须大于 0");
            }
            return pullNumber;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("GitHub PR 编号超出支持范围");
        }
    }
}
