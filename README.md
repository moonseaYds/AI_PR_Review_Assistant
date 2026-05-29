# AI PR Review Assistant

## 项目简介

本项目选择暑期实习比赛题目三：AI PR Review 助手。作品面向开发者在 Pull Request 评审中的真实需求，目标是通过 AI 辅助分析提升代码评审效率与质量。

第一阶段定位为“Spring Boot 后端 + 简单页面”的工具：用户输入 GitHub PR 链接后，系统获取 PR 变更内容，整理 diff 上下文，并调用 DeepSeek API 生成 PR 变更总结、风险代码识别和 Review 建议。

## 核心功能规划

- PR 变更总结：概括本次 PR 修改了哪些模块和行为。
- 风险代码识别：发现可能的空指针、异常处理、接口兼容性、性能、敏感信息和可维护性风险。
- Review 建议生成：给出可执行的修改建议，并标注风险等级和相关文件。
- 报告展示：通过后端接口和简单页面展示分析结果，方便 Demo 演示。

## 当前开发进度

- 已完成项目初始化、技术选型说明和密钥保护配置。
- 已完成 GitHub PR 链接解析接口，可从标准 PR 链接中提取 owner、repo 和 pull number。
- 已完成 GitHub PR 获取能力，可通过 PR 链接获取 PR 元信息（title、author、state、baseBranch、headBranch）和变更文件列表（filename、status、additions、deletions、changes、patch）。
- 已完成 Diff 上下文整理与截断能力，可将 PR 变更文件列表转换为结构化 Diff Review Context，支持单文件和总 patch 长度截断，为后续 DeepSeek AI Review 提供可控输入。
- 已完成 DeepSeek AI Review 引擎，可基于 DiffReviewContext 调用 DeepSeek API 生成结构化 Review 报告，包含 PR 变更总结、风险等级（LOW/MEDIUM/HIGH）、风险列表和 Review 建议。

## 技术选型

- 主语言：Java 17
- 后端框架：Spring Boot 3.5.14
- 构建工具：Maven
- AI 模型：DeepSeek API
- 代码平台：GitHub REST API
- 主要依赖：
  - Spring Web：提供 REST API 和静态页面能力
  - Spring Validation：处理请求参数校验
  - Lombok：减少样板代码
  - Spring Boot Test：提供单元测试和上下文测试能力

## 初步架构

```text
用户输入 PR 链接
        |
        v
PR URL 解析模块
        |
        v
GitHub API 获取 PR 元信息和变更文件
        |
        v
Diff 上下文整理与截断
        |
        v
DeepSeek AI Review 分析
        |
        v
Review 报告 API / 页面展示
```

后续模块规划：

- `github`：解析 GitHub PR 链接，调用 GitHub API 获取 PR 元数据和 changed files。
- `diff`：整理 patch、文件状态、增删行数，并处理超长 diff。
- `review`：构造模型提示词，调用 DeepSeek API，并解析结构化 Review 结果。
- `report`：组装变更总结、风险点和建议，提供给接口或页面展示。
- `common`：统一异常处理、响应结构和配置读取。

## 环境变量

真实密钥只允许保存在本地环境中，不提交到代码仓库。

```bash
export DEEPSEEK_API_KEY=你的 DeepSeek API Key
export DEEPSEEK_BASE_URL=https://api.deepseek.com
export DEEPSEEK_MODEL=deepseek-v4-flash
export GITHUB_TOKEN=可选的 GitHub Token
```

说明：

- `DEEPSEEK_API_KEY`：调用 DeepSeek API 所需，后续由本地 `.env` 或系统环境变量提供。
- `GITHUB_TOKEN`：可选，用于提高 GitHub API 限额或访问授权仓库。
- `.env`、`application-local.yml`、`application-local.properties` 已加入 `.gitignore`，不得提交真实密钥。

## 本地运行

确保本机使用 JDK 17：

```bash
java -version
mvn -v
```

运行测试：

```bash
mvn test
```

启动服务：

```bash
mvn spring-boot:run
```

默认访问地址：

```text
http://localhost:8080
```

## 当前接口

### 解析 GitHub PR 链接

```http
POST /api/reviews/parse-pr-url
Content-Type: application/json
```

请求示例：

```json
{
  "prUrl": "https://github.com/spring-projects/spring-boot/pull/12345"
}
```

响应示例：

```json
{
  "owner": "spring-projects",
  "repo": "spring-boot",
  "pullNumber": 12345,
  "normalizedUrl": "https://github.com/spring-projects/spring-boot/pull/12345"
}
```

### 获取 GitHub PR 信息

```http
POST /api/reviews/fetch-pr
Content-Type: application/json
```

请求示例：

```json
{
  "prUrl": "https://github.com/spring-projects/spring-boot/pull/12345"
}
```

成功响应示例：

```json
{
  "owner": "spring-projects",
  "repo": "spring-boot",
  "pullNumber": 12345,
  "title": "Fix login bug",
  "author": "octocat",
  "state": "open",
  "baseBranch": "main",
  "headBranch": "feature/login-fix",
  "changedFiles": [
    {
      "filename": "src/main/java/App.java",
      "status": "modified",
      "additions": 10,
      "deletions": 3,
      "changes": 13,
      "patch": "@@ -1,3 +1,10 @@ ..."
    }
  ]
}
```

非法 PR URL 返回 400：

```json
{
  "code": "BAD_REQUEST",
  "message": "当前仅支持 github.com 的 PR 链接",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

GitHub API 错误（如 404、限流）返回 502：

```json
{
  "code": "UPSTREAM_ERROR",
  "message": "GitHub PR 不存在：owner/repo#123，请检查 owner、仓库名或 PR 编号是否正确",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

### Diff 上下文整理与截断

```http
POST /api/reviews/build-diff-context
Content-Type: application/json
```

本接口用于将 PR 变更文件列表整理为结构化 Diff Review Context，对 patch 做单文件和总长度截断，为后续 DeepSeek AI Review 提供可控输入。

请求示例（小 diff）：

```json
{
  "owner": "owner",
  "repo": "repo",
  "pullNumber": 123,
  "title": "PR title",
  "changedFiles": [
    {
      "filename": "src/main/java/App.java",
      "status": "modified",
      "additions": 10,
      "deletions": 3,
      "changes": 13,
      "patch": "@@ -1,3 +1,10 @@ ..."
    }
  ]
}
```

成功响应示例（未截断）：

```json
{
  "owner": "owner",
  "repo": "repo",
  "pullNumber": 123,
  "title": "PR title",
  "totalFiles": 1,
  "totalAdditions": 10,
  "totalDeletions": 3,
  "totalChanges": 13,
  "truncated": false,
  "truncationReason": null,
  "fileContexts": [
    {
      "filename": "src/main/java/App.java",
      "status": "modified",
      "additions": 10,
      "deletions": 3,
      "changes": 13,
      "patchExcerpt": "@@ -1,3 +1,10 @@ ...",
      "patchTruncated": false
    }
  ]
}
```

超长 diff 截断响应示例：

```json
{
  "owner": "owner",
  "repo": "repo",
  "pullNumber": 456,
  "title": "Large PR",
  "totalFiles": 2,
  "totalAdditions": 200,
  "totalDeletions": 50,
  "totalChanges": 250,
  "truncated": true,
  "truncationReason": "部分文件 patch 超过单文件限制（4000 字符），已截断",
  "fileContexts": [
    {
      "filename": "BigFile.java",
      "status": "modified",
      "additions": 150,
      "deletions": 30,
      "changes": 180,
      "patchExcerpt": "@@ -1 +1 @@ ...（截断至 4000 字符）",
      "patchTruncated": true
    }
  ]
}
```

patch 为空文件的处理：

```json
{
  "filename": "binary.o",
  "status": "modified",
  "additions": 0,
  "deletions": 0,
  "changes": 0,
  "patchExcerpt": "GitHub 未返回 patch，可能是二进制文件或变更过大",
  "patchTruncated": false
}
```

changedFiles 为空时返回 400：

```json
{
  "code": "BAD_REQUEST",
  "message": "changedFiles 不能为空",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

截断规则说明：

- 单文件 patch excerpt 最大 **4000** 字符。
- 总 patch excerpt 最大 **16000** 字符。
- patch 为 `null` 或空白时返回占位说明文本。
- 截断时优先保留文件列表顺序。
- 截断后 `truncated` 标记为 `true`，`truncationReason` 给出原因。

### AI Review 分析

```http
POST /api/reviews/ai-review
Content-Type: application/json
```

本接口接收 `DiffReviewContext`，调用 DeepSeek API 对 PR 变更进行智能 Code Review，生成结构化报告。

请求示例：

```json
{
  "owner": "owner",
  "repo": "repo",
  "pullNumber": 123,
  "title": "PR title",
  "totalFiles": 1,
  "totalAdditions": 10,
  "totalDeletions": 3,
  "totalChanges": 13,
  "truncated": false,
  "truncationReason": null,
  "fileContexts": [
    {
      "filename": "src/main/java/App.java",
      "status": "modified",
      "additions": 10,
      "deletions": 3,
      "changes": 13,
      "patchExcerpt": "@@ -1,3 +1,10 @@ ...",
      "patchTruncated": false
    }
  ]
}
```

成功响应示例：

```json
{
  "summary": "本次 PR 修复了登录模块的空指针问题，代码改动范围小、逻辑清晰。",
  "riskLevel": "LOW",
  "risks": [
    {
      "file": "src/main/java/App.java",
      "level": "LOW",
      "title": "变量命名不够清晰",
      "reason": "第 12 行变量 x 未体现其业务含义",
      "suggestion": "建议将 x 改为 loginRetryCount"
    }
  ],
  "suggestions": [
    {
      "file": "src/main/java/App.java",
      "category": "可维护性",
      "content": "建议为新增的登录重试逻辑添加单元测试"
    }
  ],
  "model": "deepseek-v4-flash"
}
```

未发现风险的响应示例：

```json
{
  "summary": "本次 PR 变更范围小，代码风格一致，未发现明显风险。",
  "riskLevel": "LOW",
  "risks": [],
  "suggestions": [
    {
      "file": "src/main/java/Utils.java",
      "category": "性能",
      "content": "可考虑使用 StringBuilder 替代字符串拼接"
    }
  ],
  "model": "deepseek-v4-flash"
}
```

fileContexts 为空时返回 400：

```json
{
  "code": "BAD_REQUEST",
  "message": "fileContexts 不能为空，请先调用 /api/reviews/build-diff-context 构造上下文",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

未配置 DEEPSEEK_API_KEY 时返回 502：

```json
{
  "code": "UPSTREAM_ERROR",
  "message": "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

模型选择与 Prompt 策略说明：

- 默认使用 `deepseek-v4-flash` 模型，可通过 `DEEPSEEK_MODEL` 环境变量切换。
- 上下文获取：通过 `DiffReviewContext` 接收文件级 patch excerpt，支持截断以保证不超过模型上下文窗口。
- 误报控制：Prompt 明确要求只输出有证据支持的问题，不确定的问题放入 suggestions 而非 risks。
- 审查维度：正确性、空指针、异常处理、接口兼容性、安全性、性能、可维护性。
- 响应格式：要求模型严格输出 JSON，不包含 markdown 标记，便于程序解析。

## 原创说明

本项目为比赛作品，核心流程、后端接口、PR 分析逻辑、AI Review 提示词设计、报告结构和页面展示将围绕本次题目自主完成。使用的第三方框架和外部 API 会在本文档中列明。

## Demo 视频

Demo 视频将在核心功能完成后录制，要求包含语音讲解、主要功能演示和效果展示。

视频链接占位：

```text
待补充
```

## 后续扩展方向

- 支持 GitLab、Gitee 等更多代码平台。
- 接入 GitHub App 或 Webhook，自动触发 PR 分析。
- 支持将 Review 建议自动评论到 PR。
- 引入项目规则库，支持团队代码规范和自定义 Review 策略。
- 增强上下文获取能力，结合相关类、接口、配置文件和调用链降低误报。
- 接入 CI 流水线，在合并前输出风险报告。
