#!/usr/bin/env bash

set -u

if [ "${AI_PR_REVIEW_SKIP:-0}" = "1" ]; then
  echo "[ai-pr-review] AI_PR_REVIEW_SKIP=1，跳过提交前 Review。"
  exit 0
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "[ai-pr-review] 当前目录不在 Git 仓库中，跳过。"
  exit 0
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT" || exit 1

if git diff --cached --quiet --exit-code; then
  echo "[ai-pr-review] 没有 staged diff，跳过提交前 Review。"
  exit 0
fi

JAR_PATH="${AI_PR_REVIEW_JAR:-target/Ai_Review-0.0.1-SNAPSHOT.jar}"
MODE="${AI_PR_REVIEW_MODE:-FAST}"
ENFORCE="${AI_PR_REVIEW_ENFORCE:-0}"
OUTPUT_PATH="${AI_PR_REVIEW_OUTPUT:-}"
TMP_DIFF=""
TMP_REPORT=""

cleanup() {
  if [ -n "$TMP_DIFF" ]; then
    rm -f "$TMP_DIFF"
  fi
  if [ -n "$TMP_REPORT" ]; then
    rm -f "$TMP_REPORT"
  fi
}

run_review() {
  java -jar "$JAR_PATH" \
    --cli \
    --diff-file="$TMP_DIFF" \
    --repository="$(basename "$REPO_ROOT")" \
    --base="${AI_PR_REVIEW_BASE:-main}" \
    --head="${AI_PR_REVIEW_HEAD:-staged}" \
    --mode="$MODE"
}

trap cleanup EXIT

if [ ! -f "$JAR_PATH" ]; then
  echo "[ai-pr-review] 未找到 Jar：$JAR_PATH"
  echo "[ai-pr-review] 请先执行：mvn package"
  if [ "$ENFORCE" = "1" ]; then
    exit 1
  fi
  exit 0
fi

echo "[ai-pr-review] 正在分析 staged diff，模式：$MODE"
echo "[ai-pr-review] 如需跳过本次检查，可设置 AI_PR_REVIEW_SKIP=1。"
if [ -n "$OUTPUT_PATH" ]; then
  echo "[ai-pr-review] Review 报告将同时保存到：$OUTPUT_PATH"
fi

TMP_DIFF="$(mktemp)"
git diff --cached --no-ext-diff --unified=80 > "$TMP_DIFF"

if [ -n "$OUTPUT_PATH" ]; then
  TMP_REPORT="$(mktemp)"
  run_review > "$TMP_REPORT"
  STATUS=$?
  cat "$TMP_REPORT"
else
  run_review
  STATUS=$?
fi

if [ "$STATUS" -ne 0 ]; then
  echo "[ai-pr-review] Review 执行失败，退出码：$STATUS"
  if [ "$ENFORCE" = "1" ]; then
    echo "[ai-pr-review] 当前为强制模式，阻止本次 commit。"
    exit "$STATUS"
  fi
  echo "[ai-pr-review] 当前为提醒模式，不阻止 commit。"
fi

if [ "$STATUS" -eq 0 ] && [ -n "$OUTPUT_PATH" ] && [ -n "$TMP_REPORT" ]; then
  OUTPUT_DIR="$(dirname "$OUTPUT_PATH")"
  if [ "$OUTPUT_DIR" != "." ]; then
    mkdir -p "$OUTPUT_DIR"
  fi
  cp "$TMP_REPORT" "$OUTPUT_PATH"
  echo "[ai-pr-review] Review 报告已保存：$OUTPUT_PATH"
fi

exit 0
