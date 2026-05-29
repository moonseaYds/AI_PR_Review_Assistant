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

        const rawUrl = input.value;
        if (!validateInput(rawUrl)) {
            return;
        }

        const prUrl = rawUrl.trim();
        setState(STATE.LOADING);

        try {
            const data = await Api.analyzePR(prUrl);
            setState(STATE.SUCCESS);
            Render.result(resultArea, data);
            currentState = STATE.SUCCESS;
            button.disabled = false;
            button.textContent = "开始分析";
        } catch (err) {
            setState(STATE.ERROR);
            Render.error(resultArea, err.message);
            currentState = STATE.ERROR;
            button.disabled = false;
            button.textContent = "开始分析";
        }
    }

    form.addEventListener("submit", handleSubmit);

    // Clear error hint on input
    input.addEventListener("input", function () {
        if (urlHint.textContent) {
            urlHint.textContent = "";
        }
    });
})();
