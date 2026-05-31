package com.example.ai_review.report;

import com.example.ai_review.github.ChangedFile;
import com.example.ai_review.review.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MergeRiskAnalyzer {

    public MergeRiskReport analyze(List<ChangedFile> files) {
        if (files == null || files.isEmpty()) {
            return new MergeRiskReport(RiskLevel.LOW, "未检测到文件变更，合并风险较低。", List.of());
        }

        List<MergeRiskItem> items = new ArrayList<>();
        boolean hasMainCodeChange = false;
        boolean hasTestChange = false;
        int totalChanges = 0;

        for (ChangedFile file : files) {
            String filename = lower(file.filename());
            String patch = lower(file.patch());
            totalChanges += file.changes();

            if (isMainCode(filename)) {
                hasMainCodeChange = true;
            }
            if (isTestFile(filename)) {
                hasTestChange = true;
            }

            addDependencyRisk(items, file, filename);
            addConfigurationRisk(items, file, filename);
            addSecurityRisk(items, file, filename, patch);
            addPublicApiRisk(items, file, filename, patch);
            addCiDeployRisk(items, file, filename);
        }

        if (hasMainCodeChange && !hasTestChange) {
            items.add(new MergeRiskItem(
                    "测试覆盖",
                    "src/test",
                    "MEDIUM",
                    "本次 PR 修改了生产代码，但未检测到测试文件变更，合入 main 后缺少自动化回归保护。",
                    "建议补充单元测试、Controller 层测试或在 PR 描述中说明已有测试命令与覆盖范围。"
            ));
        }

        if (totalChanges >= 500) {
            items.add(new MergeRiskItem(
                    "变更规模",
                    "-",
                    "MEDIUM",
                    "本次 PR 总变更行数较大，人工 Review 和模型分析都更容易遗漏边界情况。",
                    "建议拆分 PR，或使用 DEEP 分批 Review 后重点复查高风险模块。"
            ));
        }

        RiskLevel riskLevel = highestRisk(items);
        String summary = buildSummary(riskLevel, items);
        return new MergeRiskReport(riskLevel, summary, items);
    }

    private void addDependencyRisk(List<MergeRiskItem> items, ChangedFile file, String filename) {
        if (filename.endsWith("pom.xml") || filename.endsWith("build.gradle")
                || filename.endsWith("build.gradle.kts")) {
            items.add(new MergeRiskItem(
                    "依赖/构建",
                    file.filename(),
                    "HIGH",
                    "构建或依赖文件发生变更，可能影响 main 分支编译、打包或运行时依赖解析。",
                    "建议合并前执行 mvn test / mvn package，并关注依赖版本冲突、插件版本和 Java 版本兼容性。"
            ));
        }
    }

    private void addConfigurationRisk(List<MergeRiskItem> items, ChangedFile file, String filename) {
        if (filename.contains("application.") || filename.contains("/config/")
                || filename.endsWith(".properties") || filename.endsWith(".yml")
                || filename.endsWith(".yaml")) {
            items.add(new MergeRiskItem(
                    "配置变更",
                    file.filename(),
                    "MEDIUM",
                    "配置文件或配置类发生变更，可能导致不同环境下启动参数、外部 API 地址或开关行为变化。",
                    "建议本地启动应用，并检查 README 中的环境变量说明是否仍然准确。"
            ));
        }
    }

    private void addSecurityRisk(List<MergeRiskItem> items, ChangedFile file, String filename, String patch) {
        if (filename.contains("security") || filename.contains("auth")
                || patch.contains("permitall") || patch.contains("csrf")
                || patch.contains("hasrole") || patch.contains("authorize")
                || patch.contains("password") || patch.contains("secret")
                || patch.contains("token")) {
            items.add(new MergeRiskItem(
                    "安全/权限",
                    file.filename(),
                    "HIGH",
                    "本次变更涉及认证、授权、敏感字段或安全配置，合并后可能扩大访问范围或暴露敏感信息。",
                    "建议重点复查权限边界，确认没有提交真实密钥，并补充安全相关测试或手动验证。"
            ));
        }
    }

    private void addPublicApiRisk(List<MergeRiskItem> items, ChangedFile file, String filename, String patch) {
        if (filename.contains("controller") || patch.contains("@getmapping")
                || patch.contains("@postmapping") || patch.contains("@requestmapping")
                || patch.contains("record ") || patch.contains("public record")) {
            items.add(new MergeRiskItem(
                    "公开接口",
                    file.filename(),
                    "MEDIUM",
                    "公开 API、Controller 或响应 DTO 发生变更，可能影响前端、调用方或 README 中的接口示例。",
                    "建议用 MockMvc、curl 或 Web Demo 验证接口响应，并同步更新接口文档。"
            ));
        }
    }

    private void addCiDeployRisk(List<MergeRiskItem> items, ChangedFile file, String filename) {
        if (filename.startsWith(".github/workflows/") || filename.contains("dockerfile")
                || filename.contains("deploy") || filename.contains("ci.")) {
            items.add(new MergeRiskItem(
                    "CI/部署",
                    file.filename(),
                    "MEDIUM",
                    "CI、部署或容器配置发生变更，可能导致自动化构建、发布或运行环境变化。",
                    "建议检查 GitHub Actions 结果，必要时本地执行等价构建命令。"
            ));
        }
    }

    private RiskLevel highestRisk(List<MergeRiskItem> items) {
        RiskLevel highest = RiskLevel.LOW;
        for (MergeRiskItem item : items) {
            RiskLevel candidate = parseRisk(item.level());
            if (rank(candidate) > rank(highest)) {
                highest = candidate;
            }
        }
        return highest;
    }

    private String buildSummary(RiskLevel riskLevel, List<MergeRiskItem> items) {
        if (items.isEmpty()) {
            return "未检测到明显合并风险，仍建议在合并前执行项目测试。";
        }
        return "检测到 " + items.size() + " 个合并风险信号，最高等级为 " + riskLevel
                + "。请在合并前重点确认依赖、配置、权限、公开接口和测试覆盖。";
    }

    private RiskLevel parseRisk(String level) {
        if ("HIGH".equalsIgnoreCase(level)) return RiskLevel.HIGH;
        if ("MEDIUM".equalsIgnoreCase(level)) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private int rank(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private boolean isMainCode(String filename) {
        return filename.startsWith("src/main/")
                || (filename.endsWith(".java") && !isTestFile(filename));
    }

    private boolean isTestFile(String filename) {
        return filename.startsWith("src/test/")
                || filename.contains("/test/")
                || filename.endsWith("test.java")
                || filename.endsWith("tests.java");
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
