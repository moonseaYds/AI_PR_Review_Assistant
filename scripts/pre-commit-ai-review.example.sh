#!/usr/bin/env bash

# 将本文件复制到 .git/hooks/pre-commit 并赋予执行权限：
# cp scripts/pre-commit-ai-review.example.sh .git/hooks/pre-commit
# chmod +x .git/hooks/pre-commit

set -u

if [ "${AI_PR_REVIEW_SKIP:-0}" = "1" ]; then
  echo "[ai-pr-review] AI_PR_REVIEW_SKIP=1，跳过提交前 Review。"
  exit 0
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"

if [ -f "$REPO_ROOT/scripts/ai-pr-review-staged.sh" ]; then
  "$REPO_ROOT/scripts/ai-pr-review-staged.sh"
else
  echo "[ai-pr-review] 未找到 scripts/ai-pr-review-staged.sh，跳过。"
fi
