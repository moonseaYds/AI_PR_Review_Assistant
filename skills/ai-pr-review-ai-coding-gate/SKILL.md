---
name: ai-pr-review-ai-coding-gate
description: Use this skill only when the user explicitly asks to run AI PR Review Assistant as an AI Coding submission gate, pre-commit review gate, HTML review report generator, or PR handoff gate for /Users/moonsea/code_project/qiniuyun_project/Ai_Review. It checks the current diff or staged diff, calls the existing CLI/Git Hook review flow, generates an HTML report, summarizes risk, and then asks the user whether to fix, rollback, force PR upload, or pause.
---

# AI PR Review AI Coding Gate

## Purpose

Use this skill as a manual AI Coding quality gate. It runs after AI-generated code changes and before commit or PR creation, then lets the user choose the next step.

Do not trigger this skill for ordinary development unless the user explicitly asks for AI Review Gate, AI Coding Skill, pre-commit AI Review, or HTML Review report generation.

## Workflow

1. Confirm the project root:

```text
/Users/moonsea/code_project/qiniuyun_project/Ai_Review
```

2. Inspect the current state:

```bash
git status --short --branch
```

3. Confirm that only this task's files are staged or intended for review.

4. Prefer staged diff. If no staged diff exists, ask the user to stage the relevant files or use a local diff command.

5. Run the Git Hook review path with an HTML report:

```bash
AI_PR_REVIEW_OUTPUT=ai-review-report.html \
AI_PR_REVIEW_OUTPUT_FORMAT=html \
scripts/ai-pr-review-staged.sh
```

6. Summarize the report path, highest risk level, and key findings for the user.

7. Present a short decision menu after review:

```text
请选择下一步：
1. 修复后重新 Review（推荐，尤其存在 HIGH 风险时）
2. 回滚本轮修改（仅在用户明确确认后执行）
3. 强制进入提交/PR 阶段（保留风险提示，由用户承担）
4. 暂停，不做提交
```

8. If the report includes HIGH risk, recommend option 1 and explain the concrete fix suggestions. Do not proceed to commit/PR unless the user explicitly chooses option 3.

9. If the user chooses option 1, make the requested fixes, rerun the same HTML review, and present the decision menu again.

10. If the user chooses option 2, ask for explicit confirmation before destructive rollback. Prefer targeted restore commands for this task's files. Never run broad destructive commands such as `git reset --hard`.

11. If the user chooses option 3, follow the repository PR workflow: precise `git add`, clear commit message, push branch, create PR, and include the HTML report path or summary in the PR description when useful.

12. If risk is LOW or MEDIUM, recommend option 3 only after summarizing remaining warnings. Still wait for explicit user approval before commit, push, PR, or merge work.

## Safety Rules

- Never pass API keys or GitHub tokens as command-line arguments.
- Never read, print, or commit `.env`, `.env.*`, or local credential files.
- Do not automatically commit, push, create PRs, or merge branches before the post-review decision menu.
- HIGH risk is blocking by default, but the user may explicitly force upload after acknowledging the risk.
- Rollback is allowed only after explicit user confirmation and must be targeted to this task's files.
- Do not include unrelated local changes in staged diff review.
- Keep `docs/CODEX_CONTEXT.md` and `docs/DEVELOPMENT_PLAN.md` local unless the user explicitly asks to submit them.

## Useful Commands

Build the jar if needed:

```bash
mvn package
```

Run staged diff review and save HTML:

```bash
AI_PR_REVIEW_OUTPUT=ai-review-report.html \
AI_PR_REVIEW_OUTPUT_FORMAT=html \
scripts/ai-pr-review-staged.sh
```

Run CLI against a patch file:

```bash
AI_PR_REVIEW_OUTPUT_FORMAT=html \
java -jar target/Ai_Review-0.0.1-SNAPSHOT.jar \
  --cli \
  --diff-file=patch.diff \
  --repository=Ai_Review \
  --base=main \
  --head=working-tree > ai-review-report.html
```

Skip the hook only when the user explicitly wants to bypass review:

```bash
AI_PR_REVIEW_SKIP=1 git commit -m "message"
```

## PR Handoff After Approval

When the user approves upload after review:

```bash
git status --short --branch
git diff --cached --name-only
git commit -m "type: concise summary"
git push -u origin <branch>
gh pr create --base main --head <branch> --title "..." --body "..."
```

The PR body should include:

- what changed
- why it changed
- review report path or key risk summary
- test commands and results
- note if the user forced upload despite HIGH risk
