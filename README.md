# AI PR Review Assistant

## 项目简介

本项目选择暑期实习比赛题目三：AI PR Review 助手，定位为面向开发者工作流的轻量 AI PR Review 工具，而非一个网站平台。

作品面向开发者在 Pull Request 评审中的真实需求，目标是通过 AI 辅助分析提升代码评审效率与质量。核心能力为：用户输入 GitHub PR 链接后，系统获取 PR 变更内容，整理 diff 上下文，并调用 DeepSeek API 生成 PR 变更总结、风险代码识别和 Review 建议。

与“功能大而全”的评审后台不同，本项目优先服务开发者提交、创建 PR、合并 PR 前的即时检查场景。历史记录、团队空间、模型模式面板等功能后续可扩展，当前阶段更关注低安装成本、低环境依赖和可嵌入开发流程的工具体验，避免为了展示功能而引入过重的平台化设计。

### 多端工具矩阵

当前 Web 页面是 Demo 入口，但不是唯一产品形态。同一套后端 Review 能力（`/api/reviews/analyze`）可服务于多种入口：

| 入口 | 形态 | 说明 |
|------|------|------|
| **Web Demo** | 静态页面（本轮已完成） | 评委和用户无需安装额外工具即可体验完整链路 |
| **IDE 插件** | IntelliJ IDEA 插件 | 开发者在看代码或提交 PR 前触发 Review |
| **浏览器插件** | Chrome/Edge 扩展 | 在 GitHub PR 页面一键分析当前 PR |
| **CLI/脚本** | 命令行工具 | 输入 PR URL 即可获取报告，适合 CI 和自动化脚本 |
| **CI Bot** | GitHub Actions / Webhook | PR 流程中自动生成 Review 报告或评论 |
| **AI Coding Skill** | Codex / Claude Code Skill | 在 AI Coding 提交、创建 PR 或合并前自动触发 Review |

各入口只负责收集 PR URL 和展示报告，核心分析逻辑统一复用后端 API，避免重复实现。

后续扩展优先级不是把 Web 页面做成复杂平台，而是让工具更贴近真实开发链路：先支持 GitHub PR Comment 或 CLI 入口，再扩展到 Git Hook、GitHub Actions 和 AI Coding Skill。

## 核心功能规划

- PR 变更总结：概括本次 PR 修改了哪些模块和行为。
- 风险代码识别：发现可能的空指针、异常处理、接口兼容性、性能、敏感信息和可维护性风险。
- Review 建议生成：给出可执行的修改建议，并标注风险等级和相关文件。
- 报告展示：通过端到端 API 和 Web Demo 页面展示分析结果，方便 Demo 演示。

## 当前开发进度

- 已完成项目初始化、技术选型说明和密钥保护配置。
- 已完成 GitHub PR 链接解析接口，可从标准 PR 链接中提取 owner、repo 和 pull number。
- 已完成 GitHub PR 获取能力，可通过 PR 链接获取 PR 元信息（title、author、state、baseBranch、headBranch）和变更文件列表（filename、status、additions、deletions、changes、patch）。
- 已完成 Diff 上下文整理与截断能力，可将 PR 变更文件列表转换为结构化 Diff Review Context，支持单文件和总 patch 长度截断，为后续 DeepSeek AI Review 提供可控输入。
- 已完成 DeepSeek AI Review 引擎，可基于 DiffReviewContext 调用 DeepSeek API 生成结构化 Review 报告，包含 PR 变更总结、风险等级（LOW/MEDIUM/HIGH）、风险列表和 Review 建议。
- 已完成端到端 Review Report API，用户只需输入 PR 链接即可一次性完成 URL 解析、PR 获取、Diff 上下文构建和 AI Review 分析的完整流程。
- 已完成简单 Web 演示页面，可作为 Demo 入口，支持输入 PR 链接、调用分析接口并展示结构化 Review 报告。
- 已完成 GitHub PR Comment 发布能力，可将 AI Review 报告发布到 GitHub PR 评论区，让分析结果留在开发协作现场。

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

### AI 模型选型

AI PR Review 场景对模型有以下核心要求：

- **代码理解与推理能力**：需要读懂跨文件 diff 并发现潜在的空指针、异常处理、接口兼容和逻辑错误，而不是简单做文本分类。
- **可控上下文**：PR 的 patch 总长度可能超过模型默认上下文窗口，因此本项目在应用层做了截断控制，而不是依赖模型原生长上下文。
- **稳定 JSON 输出**：Review 报告需要结构化，模型必须能可靠地按指定 schema 输出 JSON，不得混入 markdown 或自由文本。
- **成本与响应速度**：Demo 和日常工具使用场景不希望每次分析消耗大量费用或等待过久。
- **国内接入便利性**：比赛环境在国内，需要模型 API 可直接注册使用，接入流程相对直接。
- **API 可替换性**：通过环境变量注入模型名和 API 地址，后续换模型不需要改代码。

#### 为什么当前选择 DeepSeek

- 在代码理解和中文技术语境方面表现良好，适合做 PR Review 这类专业性较强的总结和分析。
- 推理和总结能力与成本之间达到较平衡的水平，适合比赛 Demo 和轻量工具验证阶段。
- 国内开发者注册和接入门槛较低，API 兼容 OpenAI Chat Completions 格式，迁移成本小。
- 通过 `DEEPSEEK_MODEL` 环境变量保留模型替换能力（当前默认 `deepseek-v4-flash`，也可切换为 `deepseek-chat` 等其他版本）。
- `deepseek-v4-flash` 作为轻量模型，响应速度较快，适合交互式 Demo 体验。

#### 与其他模型的定性对比

**中国大模型**（通义千问、智谱 GLM、月之暗面 Kimi、豆包等）：

| 维度 | DeepSeek | 其他中国大模型 |
|------|----------|---------------|
| 代码理解 | 较好，尤其对系统级代码和算法 | 部分模型在特定语言或框架上有优化 |
| 中文能力 | 强 | 各家中文能力普遍较强 |
| API 兼容性 | OpenAI-compatible，切换成本低 | 多数兼容，但细节存在差异 |
| 长文本处理 | 可通过应用层截断控制 | 部分模型原生支持更长上下文 |
| 生态与文档 | 文档清晰，社区活跃 | 各有优劣，需逐个评估 |
| 成本 | 具有竞争力 | 各家定价策略不同，需按场景测试 |

综上，中国大模型在本项目的代码审查场景中各有所长。DeepSeek 在工程化接入、中文代码理解和性价比方面适合当前阶段，但不是唯一可选方案。

**美国大模型**（GPT、Claude、Gemini 等）：

| 维度 | DeepSeek（当前选择） | 美国大模型 |
|------|---------------------|-----------|
| 推理与代码能力 | 较好，满足 PR Review 需求 | 通常更强，尤其在复杂逻辑和多文件上下文 |
| 接入门槛 | 国内开发者接入相对直接 | 部分服务可能涉及账号、支付、网络访问或企业权限等额外配置 |
| 比赛复现成本 | 低，评委可直接注册使用 | 高，评委可能因网络或账号问题无法运行 |
| 成本 | 价格具有竞争力 | 通常更高，部分模型按 token 计费较贵 |
| 合规 | 国内接入路径相对简单，但仍需按实际数据范围做合规评估 | 涉及数据出境和合规复杂性，需评估具体场景 |
| API 稳定性 | 适合 Demo 和轻量工具 | 成熟稳定，适合生产级应用 |

美国大模型在推理能力上通常更优，但具体效果仍取决于模型版本、提示词和代码上下文质量。本项目的比赛阶段更看重"低门槛、易复现、低成本"。在 Demo 场景下，网络稳定性和注册便利性比极致推理能力更重要。未来如果项目面向海外用户或需要更强的分析深度，可切换到 GPT 或 Claude。

> 具体价格、上下文窗口和模型榜单排名会随各厂商策略变化，以官方文档为准。以上对比侧重工程选型维度，不构成绝对优劣判断。

### Java 后端技术栈选型

#### 为什么使用 Java 17

- Java 17 是长期支持版本，生态稳定，适合比赛展示和企业后端场景。
- 与 Spring Boot 3.x 直接兼容，避免版本碎片化问题。
- 作为 Java 后端实习生作品，使用 Java 主语言能直接体现语言基础、工程能力和 JVM 生态理解。

#### 为什么使用 Spring Boot

- 快速构建规范化的 REST API：注解驱动、自动配置，减少样板代码。
- 统一处理横切关注点：参数校验、全局异常处理、响应结构标准化都在框架内完成。
- 测试友好：`@SpringBootTest` + `@AutoConfigureMockMvc` + `@MockitoBean` 可以编写覆盖 Controller、Service 和静态资源的测试，无需真实外部依赖。
- 平滑扩展：当前是单体 Web 服务，后续可扩展为插件后端、CI Bot 或企业内部服务，Spring Boot 生态对各类集成有良好支持。

#### 为什么使用 Maven

- Java 生态最常见的构建工具之一，依赖声明清晰。
- 评委和用户只需安装 JDK 17 和 Maven，即可一键运行 `mvn test` 和 `mvn spring-boot:run`。
- 不引入 Gradle 等额外构建工具，降低运行门槛。

#### 为什么当前不引入数据库

- 不引入数据库不是为了简化实现，而是基于轻量工具定位的主动取舍。核心价值发生在开发者提交、创建 PR、合并前的即时分析场景中，系统只需要根据当前 PR 链接实时获取 diff、构建上下文并生成报告。
- 相比做成带账号、历史记录和后台管理的 Web 平台，本项目更关注"安装后即可使用、接入成本低、可迁移到不同开发环境"的工具体验。
- 无状态设计让评委无需安装 MySQL、Redis 或配置连接，也降低了本地复现和 Demo 录制成本。
- 历史报告、团队空间、自定义规则配置等属于后续有状态能力。如果产品发展到团队协作或审计场景，再引入数据库和缓存层。届时可通过新增持久化模块扩展，不影响现有分析 API 的核心链路。

#### 轻量化封装方向

Java 并不会限制工具形态。当前 Spring Boot 服务可以继续作为核心分析引擎，同时向多个轻量入口封装：

- **可执行 Jar**：通过 Spring Boot 打包为 fat jar，用户配置环境变量后执行 `java -jar ai-pr-review.jar` 即可启动。
- **CLI 命令行**：后续可引入 picocli 等轻量库，提供 `ai-pr-review analyze <pr-url>`，适合本地脚本、服务器和 CI 环境。
- **Docker 镜像**：通过 `docker run --env-file .env ai-pr-review` 运行，减少本机 JDK/Maven 差异。
- **GitHub PR Comment**：分析完成后把 Review 报告发布到 PR 评论区，让结果留在开发协作现场，而不是只停留在 Web 页面。
- **AI Coding Skill**：在 Codex、Claude Code 等 AI Coding 流程中，提交或合并前自动调用 CLI 或后端 API，让 AI 写代码后的 Review 成为工作流的一部分。

因此，当前 Web Demo 只是最小可演示入口，长期目标是形成“同一套后端 Review 能力 + 多个轻量触发入口”的工具矩阵。

#### 为什么使用分层模块

当前项目按职责拆分为多个包：

| 包 | 职责 | 可替换性 |
|----|------|---------|
| `github` | PR URL 解析，GitHub API 调用 | 可替换为 Gitee、GitLab 等平台 |
| `diff` | Diff 上下文整理与截断 | 截断策略可独立调整 |
| `review` | DeepSeek 客户端、Prompt 构造、报告解析 | 可替换为其他 AI 模型 |
| `report` | 端到端编排，统一分析入口 | 下层模块替换后编排逻辑不变 |
| `common` | 统一异常处理、响应结构 | 跨模块复用 |

分层的好处：
- 每个模块职责单一，方便单独测试和替换。
- 当前只支持 GitHub 和 DeepSeek，但后续替换平台或模型时只需更换对应模块，不影响上下游。
- 与"入口可替换、核心能力复用"的产品定位一致。

#### 为什么 Web Demo 使用原生 HTML/CSS/JS

详见上方"前端技术选型"小节。核心思路：当前 Demo 阶段只需一条主流程（输入 → 分析 → 展示），原生方案降低评委运行成本和前端依赖风险，模块化结构保证可维护性。未来需要复杂交互时再引入现代前端框架。

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
Review 报告 API（端到端 / 分段均可）
        |
        v
Web Demo 页面展示（已完成）
```

后续模块规划：

- `github`：解析 GitHub PR 链接，调用 GitHub API 获取 PR 元数据和 changed files。
- `diff`：整理 patch、文件状态、增删行数，并处理超长 diff。
- `review`：构造模型提示词，调用 DeepSeek API，并解析结构化 Review 结果。
- `report`：组装变更总结、风险点和建议，提供给接口或页面展示。
- `common`：统一异常处理、响应结构和配置读取。

### 前端技术选型

当前 Web Demo 选择原生 HTML / CSS / JS 模块化结构，不引入 React / Vue / Vite，理由：

- 当前目标是比赛 Demo 和最小可用工具入口，页面只承担输入 PR 链接、调用 API、展示报告一条主流程。
- Spring Boot 已能直接托管静态资源，无需新增前端构建、部署和依赖说明。
- 原生方案能降低评委运行和理解成本，也减少提交时由前端依赖带来的环境风险。
- 页面采用清晰模块边界：API 调用（`js/api.js`）、渲染层（`js/render.js`）、状态流（`js/app.js`）和样式（`styles.css`）分离，避免写成一次性大文件。
- 不引入外部 CDN、远程字体、远程图片或第三方前端库，完全离线可用。

**未来升级条件**：当出现报告历史、规则配置、团队空间、复杂导航、可视化图表或多页面工作台时，再单独开 PR 迁移为 Vue/React + Vite 独立前端工程。届时多端入口（CLI/IDE 插件等）的展示层各自独立，但仍复用同一后端 API。

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
- `GITHUB_TOKEN`：用于提高 GitHub API 限额、访问授权仓库，以及向 PR 发布评论。如需使用发布评论功能，token 需具有目标仓库的 PR 评论权限。
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

### Web Demo 使用流程

1. 启动服务（确保已配置 `DEEPSEEK_API_KEY` 和可选的 `GITHUB_TOKEN`）。
2. 浏览器打开 `http://localhost:8080/`。
3. 在输入框中填写 GitHub PR 链接（如 `https://github.com/spring-projects/spring-boot/pull/12345`）。
4. 点击"开始分析"。
5. 等待数秒后，页面展示 PR 信息、变更统计、AI 总结、风险等级、风险列表和 Review 建议。
6. 如未配置 API Key，页面会展示后端返回的清晰错误信息。

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

- 更完整的模型选型依据见上文「AI 模型选型」和「技术选型」小节。
- 默认使用 `deepseek-v4-flash` 模型，可通过 `DEEPSEEK_MODEL` 环境变量切换。
- 上下文获取：通过 `DiffReviewContext` 接收文件级 patch excerpt，支持截断以保证不超过模型上下文窗口。
- 误报控制：Prompt 明确要求只输出有证据支持的问题，不确定的问题放入 suggestions 而非 risks。
- 审查维度：正确性、空指针、异常处理、接口兼容性、安全性、性能、可维护性。
- 响应格式：要求模型严格输出 JSON，不包含 markdown 标记，便于程序解析。

### 端到端 PR 分析

```http
POST /api/reviews/analyze
Content-Type: application/json
```

本接口是面向 Demo 和用户的统一入口。只需提供 PR 链接，系统自动串联：PR URL 解析 → GitHub PR 获取 → Diff 上下文构建 → DeepSeek AI Review，一次性返回分析报告。

请求示例：

```json
{
  "prUrl": "https://github.com/owner/repo/pull/123"
}
```

成功响应示例：

```json
{
  "owner": "owner",
  "repo": "repo",
  "pullNumber": 123,
  "title": "Fix login bug",
  "author": "octocat",
  "state": "open",
  "baseBranch": "main",
  "headBranch": "feature/login-fix",
  "totalFiles": 1,
  "totalAdditions": 10,
  "totalDeletions": 3,
  "totalChanges": 13,
  "truncated": false,
  "truncationReason": null,
  "review": {
    "summary": "本次 PR 修复了登录模块的空指针问题，代码改动范围小、逻辑清晰。",
    "riskLevel": "LOW",
    "risks": [],
    "suggestions": [],
    "model": "deepseek-v4-flash"
  }
}
```

非法 PR 链接返回 400：

```json
{
  "code": "BAD_REQUEST",
  "message": "当前仅支持 github.com 的 PR 链接",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

PR 无变更文件返回 400：

```json
{
  "code": "BAD_REQUEST",
  "message": "该 PR 没有变更文件，无法进行 AI Review",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

未配置 API Key 返回 502：

```json
{
  "code": "UPSTREAM_ERROR",
  "message": "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

说明：

- 该接口一次性完成从 PR 链接到 AI Review 报告的完整流程。
- 响应同时包含 PR 元信息（owner、repo、title、author、state、分支）、变更统计和截断信息。
- 不返回完整 patch 内容，避免响应过大。
- 所有分段接口（`parse-pr-url`、`fetch-pr`、`build-diff-context`、`ai-review`）仍可单独调用。

### 发布 PR 评论

```http
POST /api/reviews/publish-comment
Content-Type: application/json
```

本接口用于将 AI Review 报告以 Markdown 格式发布到 GitHub PR 评论区。该功能体现本项目的差异化定位：Review 结果回到 PR 协作现场，而非仅停留在 Web 页面。

**需要 `GITHUB_TOKEN`**：token 必须具有对目标仓库 PR 的评论权限。未配置或权限不足时会返回清晰错误。

请求示例（传入 `/api/reviews/analyze` 的完整分析结果，后端自动生成 Markdown 并发布）：

```json
{
  "prUrl": "https://github.com/owner/repo/pull/123",
  "analysis": {
    "owner": "owner",
    "repo": "repo",
    "pullNumber": 123,
    "title": "PR title",
    "author": "octocat",
    "state": "open",
    "baseBranch": "main",
    "headBranch": "feature/example",
    "totalFiles": 1,
    "totalAdditions": 10,
    "totalDeletions": 3,
    "totalChanges": 13,
    "truncated": false,
    "truncationReason": null,
    "review": {
      "summary": "代码质量良好",
      "riskLevel": "LOW",
      "risks": [],
      "suggestions": [],
      "model": "deepseek-v4-flash"
    }
  }
}
```

成功响应示例：

```json
{
  "owner": "owner",
  "repo": "repo",
  "pullNumber": 123,
  "commentUrl": "https://github.com/owner/repo/pull/123#issuecomment-1234567890"
}
```

未配置 GITHUB_TOKEN 返回 502：

```json
{
  "code": "UPSTREAM_ERROR",
  "message": "发布 PR 评论需要配置 GITHUB_TOKEN，请在环境变量中设置一个有目标仓库评论权限的 GitHub Token",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

analysis 缺失返回 400：

```json
{
  "code": "BAD_REQUEST",
  "message": "分析结果不能为空",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

说明：

- 评论内容为 Markdown 格式，由后端 `ReviewCommentFormatter` 根据分析结果自动生成，保证格式统一和可测试性。
- 发布必须由用户显式点击触发，不会自动发布。
- 评论末尾会标注“由 AI PR Review Assistant 自动生成，仅作辅助审查建议”。
- Web Demo 页面在分析成功后显示“发布到 PR 评论”按钮。

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
