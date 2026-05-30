/**
 * Application layer — state management and event binding.
 * State: idle → loading → success | error
 */
(function () {
    "use strict";

    const STATE = Object.freeze({
        IDLE: "idle",
        LOADING: "loading",
        SUCCESS: "success",
        ERROR: "error",
    });

    const form = document.getElementById("review-form");
    const input = document.getElementById("pr-url");
    const button = document.getElementById("submit-btn");
    const resultArea = document.getElementById("result-area");
    const urlHint = document.getElementById("url-hint");

    let currentState = STATE.IDLE;
    let lastPrUrl = "";
    let lastResult = null;

    function setState(state) {
        currentState = state;
        if (state === STATE.LOADING) {
            button.disabled = true;
            button.textContent = "分析中...";
            Render.clear(urlHint);
            Render.loading(resultArea);
        } else {
            button.disabled = false;
            button.textContent = "开始分析";
        }
    }

    function validateInput(value) {
        if (!value || value.trim() === "") {
            urlHint.textContent = "请输入 GitHub PR 链接";
            return false;
        }
        return true;
    }

    async function handleSubmit(event) {
        event.preventDefault();

        var rawUrl = input.value;
        if (!validateInput(rawUrl)) {
            return;
        }

        var prUrl = rawUrl.trim();
        setState(STATE.LOADING);

        try {
            var data = await Api.analyzePR(prUrl);
            setState(STATE.SUCCESS);
            Render.result(resultArea, data);
            lastPrUrl = prUrl;
            lastResult = data;
            currentState = STATE.SUCCESS;
            button.disabled = false;
            button.textContent = "开始分析";
            // Show publish button after successful analysis
            Render.publishForm(resultArea, prUrl, data);
        } catch (err) {
            setState(STATE.ERROR);
            Render.error(resultArea, err.message);
            currentState = STATE.ERROR;
            button.disabled = false;
            button.textContent = "开始分析";
        }
    }

    async function handlePublish() {
        if (!lastPrUrl || !lastResult) {
            return;
        }
        Render.publishLoading();
        try {
            var response = await Api.publishComment(lastPrUrl, lastResult);
            Render.publishSuccess(response.commentUrl);
        } catch (err) {
            Render.publishError(err.message);
        }
    }

    form.addEventListener("submit", handleSubmit);

    // Delegate publish button click
    resultArea.addEventListener("click", function (e) {
        if (e.target && e.target.id === "publish-btn" && !e.target.disabled) {
            handlePublish();
        }
    });

    // Clear error hint on input
    input.addEventListener("input", function () {
        if (urlHint.textContent) {
            urlHint.textContent = "";
        }
    });
})();
