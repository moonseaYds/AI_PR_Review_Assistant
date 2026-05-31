/**
 * Application layer — state management and event binding.
 * State: idle → loading → success | error
 */
(function () {
    "use strict";

    var STATE = Object.freeze({
        IDLE: "idle",
        LOADING: "loading",
        SUCCESS: "success",
        ERROR: "error",
    });

    var form = document.getElementById("review-form");
    var button = document.getElementById("submit-btn");
    var resultArea = document.getElementById("result-area");
    var urlHint = document.getElementById("url-hint");
    var diffHint = document.getElementById("diff-hint");
    var analysisModeSelect = document.getElementById("analysis-mode");

    var currentState = STATE.IDLE;
    var currentMode = "pr-url";
    var lastPrUrl = "";
    var lastResult = null;
    var lastCredentials = null;

    // --- mode switching ---
    document.querySelector(".mode-switcher").addEventListener("click", function (e) {
        if (!e.target.classList.contains("mode-btn")) return;
        var mode = e.target.getAttribute("data-mode");
        if (mode === currentMode) return;

        currentMode = mode;
        document.querySelectorAll(".mode-btn").forEach(function (b) {
            b.classList.toggle("active", b.getAttribute("data-mode") === mode);
        });
        document.getElementById("mode-pr-url").classList.toggle("hidden", mode !== "pr-url");
        document.getElementById("mode-local-diff").classList.toggle("hidden", mode !== "local-diff");
        Render.clear(resultArea);
        urlHint.textContent = "";
        if (diffHint) diffHint.textContent = "";
    });

    function setState(state) {
        currentState = state;
        if (state === STATE.LOADING) {
            button.disabled = true;
            button.textContent = "分析中...";
            Render.clear(urlHint);
            if (diffHint) Render.clear(diffHint);
            Render.loading(resultArea);
        } else {
            button.disabled = false;
            button.textContent = "开始分析";
        }
    }

    function handleSubmit(event) {
        event.preventDefault();

        if (currentMode === "pr-url") {
            var input = document.getElementById("pr-url");
            var rawUrl = input.value;
            if (!rawUrl || rawUrl.trim() === "") {
                urlHint.textContent = "请输入 GitHub PR 链接";
                return;
            }
            analyzePR(rawUrl.trim(), getAnalysisMode());
        } else {
            var textarea = document.getElementById("diff-text");
            var diffText = textarea.value;
            if (!diffText || diffText.trim() === "") {
                if (diffHint) diffHint.textContent = "请粘贴 git diff 输出";
                return;
            }
            analyzeLocalDiff(diffText.trim(), getAnalysisMode());
        }
    }

    function getAnalysisMode() {
        return analysisModeSelect && analysisModeSelect.value ? analysisModeSelect.value : "FAST";
    }

    function getRuntimeCredentials() {
        var deepSeekInput = document.getElementById("deepseek-api-key");
        var githubInput = document.getElementById("github-token");
        var credentials = {
            deepSeekApiKey: deepSeekInput ? deepSeekInput.value.trim() : "",
            githubToken: githubInput ? githubInput.value.trim() : "",
        };
        return credentials.deepSeekApiKey || credentials.githubToken ? credentials : null;
    }

    async function analyzePR(prUrl, analysisMode) {
        setState(STATE.LOADING);
        lastPrUrl = prUrl;
        lastCredentials = getRuntimeCredentials();
        try {
            var data = await Api.analyzePR(prUrl, analysisMode, lastCredentials);
            setState(STATE.SUCCESS);
            Render.result(resultArea, data);
            lastResult = data;
            currentState = STATE.SUCCESS;
            button.disabled = false;
            button.textContent = "开始分析";
            // Show publish button only for GitHub PR mode
            Render.publishForm(resultArea, prUrl, data);
        } catch (err) {
            setState(STATE.ERROR);
            Render.error(resultArea, err);
            currentState = STATE.ERROR;
            button.disabled = false;
            button.textContent = "开始分析";
        }
    }

    async function analyzeLocalDiff(diffText, analysisMode) {
        setState(STATE.LOADING);
        lastPrUrl = "";
        lastCredentials = getRuntimeCredentials();
        try {
            var data = await Api.analyzeDiff(diffText, analysisMode, lastCredentials);
            setState(STATE.SUCCESS);
            Render.result(resultArea, data);
            lastResult = data;
            currentState = STATE.SUCCESS;
            button.disabled = false;
            button.textContent = "开始分析";
            // No publish button for local diff
        } catch (err) {
            setState(STATE.ERROR);
            Render.error(resultArea, err);
            currentState = STATE.ERROR;
            button.disabled = false;
            button.textContent = "开始分析";
        }
    }

    async function handlePublish() {
        if (!lastPrUrl || !lastResult || currentMode !== "pr-url") {
            return;
        }
        Render.publishLoading();
        try {
            var response = await Api.publishComment(
                lastPrUrl, lastResult, getRuntimeCredentials() || lastCredentials);
            Render.publishSuccess(response.commentUrl);
        } catch (err) {
            Render.publishError(err);
        }
    }

    form.addEventListener("submit", handleSubmit);

    resultArea.addEventListener("click", function (e) {
        if (e.target && e.target.id === "publish-btn" && !e.target.disabled) {
            handlePublish();
        }
    });

    // Clear hints on input
    document.getElementById("pr-url").addEventListener("input", function () {
        if (urlHint.textContent) urlHint.textContent = "";
    });
    document.getElementById("diff-text").addEventListener("input", function () {
        if (diffHint && diffHint.textContent) diffHint.textContent = "";
    });
})();
