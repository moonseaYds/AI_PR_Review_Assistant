# AI PR Review Assistant Development Plan

本文记录项目后续优化计划，重点回答业务竞争力、工具化定位、模型能力、稳定性和扩展性问题。

原则：先做最能体现差异化、最容易在比赛截止前落地的能力；复杂平台化能力只做设计说明，不为了炫技牺牲轻量工具定位。

## 1. 总体判断

当前项目已经具备完整链路：

```text
GitHub PR URL -> PR 获取 -> Diff 上下文 -> AI Review -> Web 展示 -> PR 评论发布
```

下一阶段最值得优化的方向不是“做成更复杂的网站”，而是把它打磨成真正贴近开发者工作流的轻量工具：

- PR 提交前可以 Review 本地改动。
- Review 结果能定位到具体代码片段。
- 报告建议更可执行，最好附带修改示例。
- 出错时能告诉用户是 token、网络、GitHub API、模型 API 还是上下文过长的问题。
- 后续能封装为 CLI、IDE 插件、浏览器插件和 AI Coding Skill。

## 2. 优先级路线图

### P0：整理 Demo 与最终交付文档

目标：提高评委理解成本和作品完成度。

建议分支：

```text
docs/demo-script-and-final-checklist
```

内容：

- 补充 Demo 录制脚本。
- 补充最终提交检查清单。
- 在 README 显眼位置保留 Demo 视频链接。
- 说明本项目为什么不引入数据库、Redis 和复杂后台。
- 说明当前 Web Demo 只是工具入口，不是唯一产品形态。

价值：

- 工程量小。
- 直接影响评委第一印象。
- 不增加代码风险。

验收：

- README 能让评委 3 分钟内理解项目价值、运行方式和演示路径。
- Demo 脚本覆盖：输入 PR URL、生成报告、发布 PR 评论、解释轻量工具定位。

### P1：支持本地 Diff Review，提交 GitHub 前先审查

目标：解决“用户 Review 出 bug 之后，能否不用先提交 GitHub，而是在本地修改后继续 Review”的问题。

结论：这个优化非常合理，而且是项目差异化最强的方向之一。

当前 GitHub PR Review 的流程适合“已经创建 PR 后审查”。但真实开发者更需要：

```text
本地修改代码 -> 运行本地 Diff Review -> 修 bug -> 再 Review -> 没问题后提交 PR
```

建议分支：

```text
feature/local-diff-review（已完成）
```

建议能力：

- 新增接口：`POST /api/reviews/analyze-diff`
- 支持用户直接提交 diff 文本，而不依赖 GitHub PR。
- Web Demo 增加第二种入口：粘贴 diff 或上传 patch 文件。
- 后续 CLI 可执行：

```bash
git diff main...HEAD | ai-pr-review analyze-diff
```

接口草案：

```http
POST /api/reviews/analyze-diff
Content-Type: application/json
```

```json
{
  "repository": "local-project",
  "baseBranch": "main",
  "headBranch": "working-tree",
  "diffText": "diff --git ..."
}
```

实现思路：

- 新增本地 diff parser，将 raw unified diff 转换为现有 DiffReviewContext。
- 复用现有 AI Review 引擎，不重复写 Prompt 和模型调用逻辑。
- 本地 diff review 不调用 GitHub API。
- 可选支持 `repository`、`baseBranch`、`headBranch` 作为展示字段。

价值：

- 贴合“提交前检查”的真实开发场景。
- 进一步证明项目是工具，不是网页。
- 后续 CLI、IDE 插件、AI Coding Skill 都会复用这个能力。

风险：

- raw diff parser 需要控制范围，第一版只支持标准 `git diff` 输出。
- 不做完整 Git 客户端，不读取用户本地仓库，避免权限和环境复杂度。

验收：

- 用户粘贴 `git diff` 输出即可得到 Review 报告。
- 同一个本地 diff 修改后可以反复 Review。
- 不需要 GitHub token。
- 不需要提交到 GitHub。

### P1：增强 Review 报告定位能力，输出代码片段和修改示例

目标：优化风险点和 Review 建议，让用户更快定位问题。

当前报告已经有文件路径、行号、风险等级和建议，但定位还不够“像开发工具”。下一步应增加：

- 风险代码片段。
- 问题原因。
- 修改建议。
- 修改示例。
- 未来 IDE 插件可用的定位字段。

建议分支：

```text
feature/review-evidence-snippets
```

建议响应结构扩展：

```json
{
  "riskLevel": "HIGH",
  "title": "异常处理缺失",
  "filePath": "src/main/java/Example.java",
  "lineNumber": 42,
  "codeSnippet": "public void foo() { ... }",
  "reason": "这里可能抛出空指针异常，但没有判空或兜底处理。",
  "suggestion": "增加空值检查并返回明确错误。",
  "exampleFix": "if (value == null) { throw new IllegalArgumentException(...); }"
}
```

实现思路：

- DiffContextBuilder 在截取 patch 时保留相关 hunk。
- Prompt 明确要求模型只能引用 diff 中存在的代码片段。
- Review 报告 schema 增加 `codeSnippet`、`reason`、`exampleFix`。
- 前端按“风险点 -> 代码片段 -> 建议 -> 示例”展示。

注意：

- 示例修复只能作为建议，不保证可直接复制运行。
- 模型必须避免编造不在 diff 中的代码。
- 片段长度要控制，避免报告过长。

价值：

- 用户体验提升明显。
- Demo 观感更专业。
- 为 IDEA 插件的“跳转到文件和行号”提前铺结构。

验收：

- 每个风险点尽量有代码片段。
- 前端代码片段以等宽字体展示。
- 空片段时也要优雅降级。
- 单元测试覆盖 JSON 解析和页面渲染。

### P1：增加错误诊断和兜底策略

目标：API、token、网络或模型异常时，工具能给出可理解的处理建议。

建议分支：

```text
feature/error-diagnostics
```

需要覆盖的错误：

- `DEEPSEEK_API_KEY` 未配置。
- DeepSeek API 超时、限流、返回格式异常。
- `GITHUB_TOKEN` 未配置或权限不足。
- GitHub API 404、403 rate limit、网络不可达。
- Diff 过大导致截断严重。
- 模型输出不是合法 JSON。

建议响应结构：

```json
{
  "errorCode": "GITHUB_TOKEN_FORBIDDEN",
  "message": "GitHub Token 权限不足，无法访问该 PR。",
  "suggestion": "请确认 token 具有目标仓库读权限，私有仓库需要 repo 权限。",
  "retryable": false
}
```

兜底策略：

- GitHub token 缺失时：公开仓库尝试匿名读取；私有仓库提示配置 token。
- GitHub API 失败时：提示用户改用本地 diff review。
- 模型 API 失败时：保留已获取的 PR 信息和 diff 统计，提示稍后重试。
- 模型 JSON 解析失败时：可以返回原始摘要文本，标记为非结构化结果。
- Diff 截断严重时：在报告顶部提示“可能遗漏部分风险”。

价值：

- 显著提升工具可用性。
- 避免 Demo 现场一出错就看不懂。
- 体现后端工程健壮性。

验收：

- 主要异常都有稳定错误码。
- 前端错误提示能告诉用户下一步怎么做。
- 测试覆盖 token 缺失、权限不足、API 失败、模型输出异常。

### P2：优化 Diff 截断策略，减少关键信息缺失

目标：回答“Diff 截断依据是什么，能否扩大，是否会缺失关键信息”。

当前截断的必要性：

- 模型上下文有限。
- 大 PR 可能包含成千上万行 patch。
- 不截断会导致请求失败、费用上升或响应很慢。

问题：

- 简单按字符数截断可能遗漏关键代码。
- 大文件靠前片段被保留，靠后风险被丢弃。

建议分支：

```text
feature/smart-diff-selection
```

改进策略：

- 保留所有文件的元信息，即使 patch 被截断。
- 优先保留高风险文件：
  - `controller`
  - `service`
  - `security`
  - `auth`
  - `config`
  - `exception`
  - `sql`
  - `pom.xml`
  - `application.yml`
- 优先保留高风险变更：
  - 删除校验逻辑
  - 修改权限判断
  - 修改异常处理
  - 修改配置和依赖
  - 新增外部请求
  - 新增文件读写
- 每个文件按 hunk 截取，而不是纯字符截取。
- 给每个文件打分，按风险分配上下文预算。

是否扩大截断上限：

- 可以扩大，但不要无限扩大。
- 建议通过配置控制：

```properties
review.diff.max-total-chars=24000
review.diff.max-file-chars=6000
```

同时保留默认轻量配置，避免请求过慢和费用失控。

验收：

- README 说明截断依据。
- 响应中明确返回 `truncated=true/false` 和截断原因。
- 大 PR 中所有文件统计仍可见。
- 高风险文件优先进入模型上下文。

### P2：增强全局代码把控与合并风险分析

目标：回答“AI 是否有全局代码把控能力，是否能分析合并后主分支跑不起来的风险”。

现实判断：

- 只看 PR diff 时，AI 没有完整仓库上下文。
- 它能发现 diff 内部的明显风险，但不能保证主分支合并后一定可运行。
- 要分析合并风险，需要结合更多上下文和自动化验证。

建议分支：

```text
feature/merge-risk-check
```

可落地能力：

- 在报告中新增“合并风险”维度：
  - 是否修改依赖。
  - 是否修改启动配置。
  - 是否修改公共接口。
  - 是否修改测试用例。
  - 是否删除或重命名类、方法、配置项。
- 增加“建议验证命令”：

```bash
mvn test
mvn spring-boot:run
```

- 后续 CLI 模式可在本地真实执行测试命令，并把结果作为上下文传给 AI。

未来能力：

- GitHub Actions 集成，读取 CI 状态。
- 本地 CLI 执行 `mvn test` 后再调用 Review。
- IDEA 插件读取当前项目结构和编译状态。

验收：

- 报告中出现“合并风险”小节。
- AI 明确区分“代码风险”和“需要通过测试验证的运行风险”。
- 不夸大模型能力，不承诺一定能判断合并后可运行。

### P2：模型供应商可切换与国外模型接入方案

目标：回答“国外模型 API 是否走代理转发，后续能否扩展切换模型能力”。

当前项目已经通过环境变量保留了基础可替换性：

- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`
- `DEEPSEEK_API_KEY`

但严格来说，现在还是 DeepSeek Client，不是完整的多模型抽象。

建议分支：

```text
feature/model-provider-abstraction
```

设计方向：

- 抽象接口：

```java
public interface AiReviewModelClient {
    ReviewReport review(DiffReviewContext context);
}
```

- Provider 实现：
  - `DeepSeekReviewModelClient`
  - `OpenAiCompatibleReviewModelClient`
  - 未来可扩展 `ClaudeReviewModelClient`、`GeminiReviewModelClient`
- 配置：

```properties
review.model.provider=deepseek
review.model.base-url=https://api.deepseek.com
review.model.name=deepseek-v4-flash
```

国外模型接入方式：

- 如果用户能直接访问官方 API，优先使用官方 API。
- 如果网络或账号渠道不稳定，可以使用合规的 API 网关或代理转发服务。
- 代理转发只解决连接和统一鉴权问题，不应把真实密钥写入代码。
- README 中可以说明：美国大模型通常推理强，但成本更高、渠道和网络配置更复杂，当前比赛阶段选择 DeepSeek 更利于复现。

验收：

- README 增加“模型可替换设计”。
- 代码层从具体 DeepSeek Client 逐步抽象为模型客户端接口。
- 不在本阶段强行接入多个真实模型，避免引入不可控依赖。

### P3：其他端工具封装

目标：回答“什么时候进行其他端工具封装”。

建议顺序：

1. 先做本地 Diff Review。
2. 再做 CLI。
3. 再做 AI Coding Skill 设计。
4. 最后考虑 IDEA 插件或浏览器插件。

原因：

- CLI 是最轻量、最贴近“工具”的入口。
- CLI 可以复用 `analyze-diff` 和 `analyze` API。
- AI Coding Skill 可以通过 CLI 或 HTTP API 调用工具。
- IDEA 插件展示效果好，但开发成本和调试成本较高，不适合在核心能力未稳定前优先做。

建议分支：

```text
feature/cli-entry
docs/ai-coding-skill-design
docs/idea-plugin-design
```

CLI 第一版目标：

```bash
ai-pr-review analyze https://github.com/owner/repo/pull/123
ai-pr-review analyze-diff < patch.diff
```

AI Coding Skill 第一版目标：

- 在 AI 准备提交代码前，自动生成 diff。
- 调用本工具分析。
- 如果存在 HIGH 风险，提醒用户先修复。
- 如果没有 HIGH 风险，再允许进入 commit 或 PR 阶段。

### P3：代码健壮性与必要中文注释

目标：提升后续维护体验，但避免把注释写成噪音。

建议：

- 对复杂流程加少量中文注释：
  - Diff 截断策略。
  - Prompt 约束。
  - 模型 JSON 解析兜底。
  - GitHub API 错误转换。
  - PR Comment Markdown 格式化。
- 对简单 getter、DTO、Controller 映射不加注释。
- README 和开发文档负责解释架构，代码注释只解释“不直观的决策”。

建议分支：

```text
refactor/code-comments-and-error-boundaries
```

验收：

- 关键类读起来能理解为什么这样做。
- 注释不重复代码本身。
- 不做大范围重构。

## 3. 竞争力提升策略

面对功能更多的对手，本项目不建议硬拼“后台功能数量”。更好的差异化叙事是：

```text
别人做 Review 平台，我做开发者提交前、PR 中、AI Coding 流程里的轻量工具。
```

建议在 Demo 和 README 中强调：

- 支持 PR URL Review。
- 支持 PR 评论发布，让结果进入协作现场。
- 计划支持本地 Diff Review，提交 GitHub 前就能自查。
- 无数据库、无复杂依赖，运行成本低。
- 可扩展为 CLI、IDE 插件、浏览器插件、GitHub Actions 和 AI Coding Skill。
- 模块分层清晰，平台、模型、入口都可替换。

如果截止前只能再做 1 个功能，优先做：

```text
本地 Diff Review
```

如果能再做 2 个功能：

```text
本地 Diff Review + 代码片段定位和修改示例
```

如果能再做 3 个功能：

```text
本地 Diff Review + 代码片段定位和修改示例 + 错误诊断兜底
```

## 4. 推荐执行顺序

### 第 1 步：文档 PR

```text
docs/demo-script-and-final-checklist
```

目的：

- 先把 Demo 讲清楚。
- 把本文路线图同步到 README 或引用到 README。
- 完成最终提交检查清单。

### 第 2 步：本地 Diff Review

```text
feature/local-diff-review（已完成）
```

目的：

- 让用户在提交 GitHub 前就能反复 Review。
- 奠定 CLI 和 AI Coding Skill 基础。

### 第 3 步：代码片段和示例修复

```text
feature/review-evidence-snippets
```

目的：

- 提升报告可读性和可执行性。
- 为 IDEA 插件定位代码做准备。

### 第 4 步：错误诊断兜底

```text
feature/error-diagnostics
```

目的：

- 提升 Demo 稳定性和真实可用性。
- 减少 token、网络、模型 API 异常带来的挫败感。

### 第 5 步：智能 Diff 截断

```text
feature/smart-diff-selection
```

目的：

- 减少大 PR 下关键信息缺失。
- 提升 AI 分析准确性。

### 第 6 步：模型抽象和多端设计

```text
feature/model-provider-abstraction
docs/ai-coding-skill-design
feature/cli-entry
```

目的：

- 强化未来扩展方向。
- 逐步落地工具矩阵。

## 5. 暂不优先做的功能

以下功能有价值，但不建议比赛当前阶段优先实现：

- 数据库历史记录。
- 用户账号体系。
- 团队空间。
- 多模型可视化控制台。
- 多仓库批量后台扫描。
- 完整 IDEA 插件。
- 完整浏览器插件商店发布。

原因：

- 会显著增加实现和测试成本。
- 容易偏离“轻量工具”定位。
- 对核心 Review 质量提升不如本地 diff 和代码片段定位直接。

这些功能可以写入未来规划，不必在截止前硬做。

