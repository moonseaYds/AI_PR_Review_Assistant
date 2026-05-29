# AI PR Review Assistant

## 项目简介

本项目选择暑期实习比赛题目三：AI PR Review 助手。作品面向开发者在 Pull Request 评审中的真实需求，目标是通过 AI 辅助分析提升代码评审效率与质量。

第一阶段定位为“Spring Boot 后端 + 简单页面”的工具：用户输入 GitHub PR 链接后，系统获取 PR 变更内容，整理 diff 上下文，并调用 DeepSeek API 生成 PR 变更总结、风险代码识别和 Review 建议。

## 核心功能规划

- PR 变更总结：概括本次 PR 修改了哪些模块和行为。
- 风险代码识别：发现可能的空指针、异常处理、接口兼容性、性能、敏感信息和可维护性风险。
- Review 建议生成：给出可执行的修改建议，并标注风险等级和相关文件。
- 报告展示：通过后端接口和简单页面展示分析结果，方便 Demo 演示。

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
