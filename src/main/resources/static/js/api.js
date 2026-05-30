/**
 * API layer — encapsulates the call to POST /api/reviews/analyze.
 */
const Api = (() => {
    const ANALYZE_URL = "/api/reviews/analyze";
    const PUBLISH_URL = "/api/reviews/publish-comment";

    /**
     * Analyze a GitHub PR by URL.
     * @param {string} prUrl
     * @returns {Promise<object>} parsed JSON response body
     * @throws {Error} with backend message when the response is not 2xx
     */
    async function analyzePR(prUrl) {
        let response;
        try {
            response = await fetch(ANALYZE_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ prUrl }),
            });
        } catch (e) {
            throw new Error("网络请求失败，请检查网络连接和后端服务是否启动");
        }

        if (!response.ok) {
            let message = "服务器返回错误 (" + response.status + ")";
            try {
                const body = await response.json();
                if (body && body.message) {
                    message = body.message;
                }
            } catch (_) {
                // Response body is not JSON; use default message
            }
            throw new Error(message);
        }

        return response.json();
    }

    /**
     * Publish a comment to a GitHub PR.
     * @param {string} prUrl
     * @param {object} analysis - AnalyzePullRequestResponse from /analyze
     * @returns {Promise<object>} parsed JSON with commentUrl
     */
    async function publishComment(prUrl, analysis) {
        let response;
        try {
            response = await fetch(PUBLISH_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ prUrl, analysis }),
            });
        } catch (e) {
            throw new Error("网络请求失败，请检查网络连接和后端服务是否启动");
        }

        if (!response.ok) {
            let message = "服务器返回错误 (" + response.status + ")";
            try {
                const respBody = await response.json();
                if (respBody && respBody.message) {
                    message = respBody.message;
                }
            } catch (_) {
                // ignore
            }
            throw new Error(message);
        }

        return response.json();
    }

    return { analyzePR, publishComment };
})();
