/**
 * API layer — encapsulates calls to backend endpoints.
 */
const Api = (() => {
    const ANALYZE_URL = "/api/reviews/analyze";
    const ANALYZE_DIFF_URL = "/api/reviews/analyze-diff";
    const PUBLISH_URL = "/api/reviews/publish-comment";

    async function handleResponse(response) {
        if (!response.ok) {
            var error = { message: "服务器返回错误 (" + response.status + ")" };
            try {
                var body = await response.json();
                error.code = body.code || "";
                error.message = body.message || error.message;
                error.suggestion = body.suggestion || "";
                error.retryable = body.retryable === true;
            } catch (_) {
                // body not JSON, use default
            }
            throw error;
        }
        return response.json();
    }

    async function analyzePR(prUrl, analysisMode) {
        var response;
        try {
            response = await fetch(ANALYZE_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ prUrl, analysisMode }),
            });
        } catch (e) {
            throw { message: "网络请求失败，请检查网络连接和后端服务是否启动",
                     code: "NETWORK_ERROR", retryable: true };
        }
        return handleResponse(response);
    }

    async function analyzeDiff(diffText, analysisMode) {
        var response;
        try {
            response = await fetch(ANALYZE_DIFF_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    repository: "local-project",
                    baseBranch: "main",
                    headBranch: "working-tree",
                    analysisMode,
                    diffText,
                }),
            });
        } catch (e) {
            throw { message: "网络请求失败，请检查网络连接和后端服务是否启动",
                     code: "NETWORK_ERROR", retryable: true };
        }
        return handleResponse(response);
    }

    async function publishComment(prUrl, analysis) {
        var response;
        try {
            response = await fetch(PUBLISH_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ prUrl, analysis }),
            });
        } catch (e) {
            throw { message: "网络请求失败，请检查网络连接和后端服务是否启动",
                     code: "NETWORK_ERROR", retryable: true };
        }
        return handleResponse(response);
    }

    return { analyzePR, analyzeDiff, publishComment };
})();
