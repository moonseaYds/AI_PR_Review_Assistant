# Codex Context Restore

本文用于在更换 Codex 账号、切换设备、切换 AI Coding 工具后，快速恢复本项目上下文。

新账号接手时，优先阅读本文和项目根目录下的 `README.md`，再继续开发。

## 1. 项目基本信息

- 项目名称：AI PR Review Assistant
- 比赛方向：题目三，AI PR Review 助手
- 项目目录：`/Users/moonsea/code_project/qiniuyun_project/Ai_Review`
- GitHub 仓库：`https://github.com/moonseaYds/AI_PR_Review_Assistant.git`
- 主语言：Java
- 用户偏好：中文沟通，中文文档优先

## 2. 产品定位

本项目不是一个重型 Web 平台，而是面向开发者工作流的轻量 AI PR Review 工具。

核心目标：

- 输入 GitHub PR 链接。
- 自动获取 PR 元信息和 changed files。
- 构建可控的 diff review context。
- 调用 DeepSeek API 生成结构化 Review 报告。
- 支持在 Web Demo 中查看报告。
- 支持将报告发布到 GitHub PR 评论区。

产品差异化方向：

- 不为了展示功能而堆数据库、Redis、历史记录后台。
- 当前保持无状态分析链路，降低评委运行成本。
- 后续重点走轻量工具矩阵：CLI、IDE 插件、浏览器插件、GitHub Actions、AI Coding Skill。

## 3. 当前技术栈

- Java 17
- Spring Boot 3.5.14
- Maven
- DeepSeek API
- GitHub REST API
- 原生 HTML / CSS / JS Web Demo
- JUnit 5 / Spring Boot Test / MockMvc / MockRestServiceServer

本机测试时曾使用：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home mvn test
```

虽然本机命令使用 JDK 25 运行 Maven，但项目 `pom.xml` 目标版本仍是 Java 17。

## 4. 环境变量与安全约束

真实密钥只允许保存在本地环境或 `.env` 中，不允许提交到仓库。

常用环境变量：

```bash
export DEEPSEEK_API_KEY=你的 DeepSeek API Key
export DEEPSEEK_BASE_URL=https://api.deepseek.com
export DEEPSEEK_MODEL=deepseek-v4-flash
export GITHUB_TOKEN=可选的 GitHub Token
```

安全要求：

- 不读取、不打印、不提交 `.env`。
- 不提交真实 API key 或 GitHub token。
- `.env`、`.env.*`、`application-local.yml`、`application-local.properties` 必须保持忽略。
- GitHub 评论发布功能只在用户明确要求时才真实调用。

## 5. 已完成 PR 记录

当前已完成并合并的主要 PR：

| PR | 分支 | 内容 |
|----|------|------|
| #1 | `feature/init-project` | 项目初始化与 README 基线 |
| #2 | `feature/github-pr-link-parser` | GitHub PR 链接解析 |
| #3 | `feature/github-pr-fetch` | 获取 PR 元信息和 changed files |
| #4 | `feature/diff-parser` | Diff 上下文构建与截断 |
| #5 | `feature/ai-review-engine` | DeepSeek AI Review 引擎 |
| #6 | `feature/review-report-api` | 端到端分析 API |
| #7 | `feature/simple-review-ui` | 简单 Web Demo |
| #8 | `docs/readme-model-tech-selection` | 模型选择、技术栈和轻量工具定位说明 |
| #9 | `feature/github-pr-comment` | 发布 AI Review 报告到 GitHub PR 评论区 |

后续继续保持小粒度 PR，每个 PR 只做一个明确目标。

## 6. 当前核心接口

### 分析 PR

```http
POST /api/reviews/analyze
Content-Type: application/json
```

请求示例：

```json
{
  "prUrl": "https://github.com/owner/repo/pull/123"
}
```

用途：

- 解析 PR URL。
- 获取 GitHub PR 信息和文件变更。
- 构建 diff context。
- 调用 AI 模型生成 Review 报告。

### 发布评论

```http
POST /api/reviews/publish-comment
Content-Type: application/json
```

用途：

- 接收 PR URL 和已生成的分析结果。
- 后端用 `ReviewCommentFormatter` 转为 Markdown。
- 调用 GitHub Issues Comments API 发布到 PR 评论区。

注意：

- 发布评论需要 `GITHUB_TOKEN`。
- 前端必须由用户显式点击发布按钮，不能自动发布。

## 7. 协作流程

本项目目前采用：

```text
Codex 写交接文档 -> Claude Code 实现 -> Claude Code 写汇报 -> Codex 审查 -> 用户确认 -> 提交 PR
```

具体规则：

- Codex 负责需求拆解、交接文档、审查和 PR 流程。
- Claude Code 负责按交接文档编码、跑测试、写本轮工作汇报。
- `CLAUDE_CODE_HANDOFF.md` 是临时文件，不提交进仓库。
- 用户说“看汇报”时，Codex 先读 `CLAUDE_CODE_HANDOFF.md` 末尾汇报，再检查代码。
- 用户说“进入提交 PR 阶段”时，Codex 才提交、推送、创建 PR。

## 8. 本地 Skill 信息

当前有两个关键 Codex skill。

### 比赛流程 Skill

路径：

```text
/Users/moonsea/.codex/skills/ai-pr-review-contest-flow/SKILL.md
```

作用：

- 约束比赛项目的 GitHub 分支、commit、PR 和 README 流程。
- 避免最后一天一次性提交导致作品无效。
- 要求 PR 描述包含功能描述、实现思路、测试方式、原创与复用说明。

### Claude Code 交接 Skill

路径：

```text
/Users/moonsea/.codex/skills/claude-handoff-loop/SKILL.md
```

作用：

- 让 Codex 生成 `CLAUDE_CODE_HANDOFF.md`。
- 让 Claude Code 按交接单开发。
- 让 Claude Code 在文档末尾追加“本轮工作汇报”。
- 让 Codex 基于汇报和代码进行审查。
- 功能完成后删除临时交接文档。

如果更换 Codex 账号后 skill 没有自动出现，可以让新账号先阅读本文件，并按这里的流程继续工作；同一台电脑上，上述 skill 文件通常仍在本地。

## 9. 给新 Codex 账号的恢复提示词

更换 Codex 账号后，在项目目录下输入：

```text
请先阅读 /Users/moonsea/code_project/qiniuyun_project/Ai_Review/docs/CODEX_CONTEXT.md 和 /Users/moonsea/code_project/qiniuyun_project/Ai_Review/README.md，恢复本项目上下文。

这是一个 AI PR Review Assistant 比赛项目，主语言为 Java，要求中文沟通。请继续遵循“小粒度功能分支 + 持续 commit + PR 描述完整 + README 同步更新”的比赛流程。不要读取或打印 .env，不要提交 API Key 或 GitHub Token。

后续协作流程是：Codex 先写 CLAUDE_CODE_HANDOFF.md 交接单，Claude Code 实现并追加“本轮工作汇报”，Codex 再审查汇报和代码。只有当我明确说“进入提交 PR 阶段”时，才进行 commit、push 和创建 PR。
```

## 10. 让 Codex 生成 Claude Code 交接单的提示词

当准备开发新功能时，对 Codex 说：

```text
按我们的流程来。请基于 docs/CODEX_CONTEXT.md 和 README.md，先为下一轮功能创建 CLAUDE_CODE_HANDOFF.md，不要直接写业务代码。

交接单需要包含：本轮目标、当前上下文、涉及模块、接口设计、边界约束、禁止事项、验收清单、测试要求。要求 Claude Code 完成后在文件末尾追加“本轮工作汇报”。
```

## 11. 给 Claude Code 的执行提示词

当 Codex 已经生成 `CLAUDE_CODE_HANDOFF.md` 后，把下面这段发给 Claude Code：

```text
请阅读项目根目录下的 CLAUDE_CODE_HANDOFF.md，按文档要求完成本轮开发。完成后必须在文件末尾追加“本轮工作汇报”，写清楚实际修改文件、完成内容、未完成内容、编译/构建/测试命令与结果、接口或页面验证结果、遇到的问题和建议下一步。
```

如果已经是第二轮或后续补充需求，则发：

```text
请继续阅读项目根目录下的 CLAUDE_CODE_HANDOFF.md，从最新追加的需求章节开始处理。完成后必须在文件末尾继续追加“本轮工作汇报”，写清楚实际修改文件、完成内容、未完成内容、编译/构建/测试命令与结果、接口或页面验证结果、遇到的问题和建议下一步。
```

## 12. 后续推荐计划

详细路线图见：

```text
docs/DEVELOPMENT_PLAN.md
```

优先级建议：

1. `docs/demo-script-and-final-checklist`
   - 补充 Demo 录制脚本。
   - 补充最终提交检查清单。
   - 确保 README 的 Demo 视频链接位置显眼。

2. `feature/local-diff-review`
   - 支持用户提交 GitHub 前直接 Review 本地 diff。
   - 为 CLI、IDE 插件和 AI Coding Skill 打基础。

3. `feature/review-evidence-snippets`
   - 在风险点和 Review 建议中展示代码片段、原因和修改示例。
   - 为未来 IDEA 插件跳转定位做准备。

4. `feature/error-diagnostics`
   - 增强 GitHub token、模型 API、网络异常和上下文截断的诊断与兜底提示。

5. `feature/smart-diff-selection`
   - 优化 Diff 截断依据，优先保留高风险文件和高风险 hunk。

6. `feature/cli-entry` / `docs/ai-coding-skill-design`
   - 增加轻量 CLI 或 AI Coding Skill 设计，体现工具化定位。

## 13. 提交前检查清单

每次提交前检查：

```bash
git status --short --branch
git remote -v
mvn test
```

必须确认：

- 当前不在 `main` 上直接开发功能。
- 只提交本轮相关文件。
- `CLAUDE_CODE_HANDOFF.md` 不进入 commit。
- `.env` 和本地配置不进入 commit。
- README 或文档已同步更新。
- PR 描述包含功能描述、实现思路、测试方式、原创与复用说明。
