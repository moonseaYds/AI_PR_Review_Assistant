# AI Coding Skill Design

## 设计目标

AI Coding 已经能快速生成代码，但“生成完成”不等于“可以提交”。本项目将 AI PR Review Assistant 封装为 AI Coding 流程中的提交前质量门：在 Codex、Claude Code 等工具准备提交代码前，先分析本轮 diff，生成可阅读的 Review 报告，再由开发者决定是否继续 commit 或 PR。

这个 Skill 不替代人工 Review，也不默认自动合并代码。它的目标是把已有的本地 Diff Review、CLI、Git Hook 和 HTML 报告能力接入 AI Coding 工作流，并在 Review 后提供“修复、回滚、强制上传、暂停”的人工确认闭环。

## 工作流

```text
AI Coding 修改代码
-> 生成或暂存本轮 diff
-> 手动触发 AI PR Review Gate
-> 调用 CLI 或 Git Hook 分析 diff
-> 生成静态 HTML Review 报告
-> 输出风险摘要和报告路径
-> 用户选择修复 / 回滚 / 强制进入 PR / 暂停
-> 风险可接受或用户强制确认后再进入 commit / PR 阶段
```

推荐命令：

```bash
git add <changed-files>
AI_PR_REVIEW_OUTPUT=ai-review-report.html \
AI_PR_REVIEW_OUTPUT_FORMAT=html \
scripts/ai-pr-review-staged.sh
```

如果不使用 staged diff，也可以直接调用 CLI：

```bash
AI_PR_REVIEW_OUTPUT_FORMAT=html \
java -jar target/Ai_Review-0.0.1-SNAPSHOT.jar \
  --cli \
  --diff-file=patch.diff \
  --repository=my-repo \
  --base=main \
  --head=working-tree > ai-review-report.html
```

## Skill 职责边界

- 只在用户明确要求时触发，不自动介入所有开发任务。
- 可以检查 git 状态、提醒用户暂存本轮相关文件、调用 Review 脚本并总结报告结果。
- 如果报告中出现 HIGH 风险，应默认暂停提交，并提示开发者或 AI Coding 工具修复。
- 用户可以选择强制进入提交 / PR 阶段，但 Skill 必须明确提示风险，并在 PR 描述中保留“强制上传”的说明。
- 用户可以选择回滚本轮修改，但必须二次确认，且只能使用针对本轮文件的定向恢复命令，不能使用 `git reset --hard`。
- 不自动 commit、push、创建 PR 或 merge，除非用户在 Review 后明确选择对应操作。
- 不通过命令行传入 API Key 或 GitHub Token，避免密钥进入 shell history。
- 不读取、不打印、不提交 `.env`、`.env.*`、本地上下文文档或临时交接文件。

## Review 后决策菜单

每次生成 HTML 报告后，Skill 都应向用户展示下面的选择：

```text
请选择下一步：
1. 修复后重新 Review（推荐，尤其存在 HIGH 风险时）
2. 回滚本轮修改（仅在用户明确确认后执行）
3. 强制进入提交/PR 阶段（保留风险提示，由用户承担）
4. 暂停，不做提交
```

推荐策略：

- HIGH 风险：默认推荐“修复后重新 Review”，不主动提交。
- MEDIUM 风险：允许进入 PR，但要摘要提醒风险点和建议验证命令。
- LOW 风险：可以建议进入 PR 阶段，但仍等待用户明确确认。
- 强制上传：允许，但需要在最终汇报或 PR 描述中标明“用户确认强制上传”。

## 复用现有能力

- 本地 Diff Review：无需先创建 GitHub PR，即可分析本地改动。
- CLI：支持 PR URL、stdin diff、patch 文件输入。
- Git Hook：支持 commit 前读取 staged diff。
- HTML 报告：通过 `AI_PR_REVIEW_OUTPUT_FORMAT=html` 生成可打开的静态报告。
- FAST / DEEP 模式：FAST 适合日常提交前自查，DEEP 适合大 diff 或关键模块。
- 合并风险分析：对依赖、配置、权限、接口等高风险变更做额外提示。
- 证据片段和示例修复：便于 AI Coding 工具继续修复问题。

## 手动触发提示词

```text
请使用 AI PR Review Assistant 的 AI Coding Skill，在提交前检查本次改动，并生成 HTML Review 报告。如果存在 HIGH 风险，请先给出修复建议并暂停提交。
```

更完整的提示词：

```text
请使用 AI PR Review Assistant 的 AI Coding Skill 检查本次改动：
1. 先查看 git status，确认当前分支和改动文件。
2. 只分析本轮相关 diff，不要混入无关文件。
3. 优先使用 staged diff；如果没有 staged diff，请提醒我先 git add。
4. 调用 scripts/ai-pr-review-staged.sh，并设置 AI_PR_REVIEW_OUTPUT=ai-review-report.html、AI_PR_REVIEW_OUTPUT_FORMAT=html。
5. 生成报告后总结风险等级和关键建议。
6. 如果存在 HIGH 风险，先暂停提交并给出修复建议。
7. 然后让我选择：修复后重新 Review、回滚本轮修改、强制进入 PR 阶段、或暂停。
8. 只有当我明确选择进入 PR 阶段时，才提交、推送并创建 PR。
```

## 后续扩展

- 与浏览器插件联动，在 GitHub PR 页面直接触发分析并打开 HTML 报告。
- 与 CI 集成，在 PR 创建或更新时自动运行 Review。
- 与 IDE 插件联动，根据报告里的文件路径、行号和代码片段跳转到对应位置。
- 结合测试命令结果，把 `mvn test`、CI 状态和 Review 报告放入同一份提交前检查结果。
