---
name: ai-pr-review-ai-coding-gate
description: Use this skill only when the user explicitly asks to run AI PR Review Assistant as an AI Coding submission gate, pre-commit review gate, or HTML review report generator for /Users/moonsea/code_project/qiniuyun_project/Ai_Review. It checks the current diff or staged diff, calls the existing CLI/Git Hook review flow, generates an HTML report when requested, and pauses commit/PR work when HIGH risks are found.
---

# AI PR Review AI Coding Gate

## Purpose

Use this skill as a manual AI Coding quality gate. It runs after AI-generated code changes and before commit or PR creation.

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

7. If the report includes HIGH risk, stop before commit/PR and propose fixes.

8. If risk is LOW or MEDIUM, wait for explicit user approval before commit, push, PR, or merge work.

## Safety Rules

- Never pass API keys or GitHub tokens as command-line arguments.
- Never read, print, or commit `.env`, `.env.*`, or local credential files.
- Do not automatically commit, push, create PRs, or merge branches.
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
