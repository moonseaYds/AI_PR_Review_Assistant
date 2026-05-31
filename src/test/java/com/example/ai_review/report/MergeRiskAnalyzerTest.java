package com.example.ai_review.report;

import com.example.ai_review.github.ChangedFile;
import com.example.ai_review.review.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeRiskAnalyzerTest {

    private final MergeRiskAnalyzer analyzer = new MergeRiskAnalyzer();

    @Test
    void detectsDependencySecurityApiAndMissingTestRisks() {
        MergeRiskReport report = analyzer.analyze(List.of(
                new ChangedFile("pom.xml", "modified", 5, 1, 6,
                        "+<dependency>\n+<artifactId>spring-security</artifactId>"),
                new ChangedFile("src/main/java/com/example/SecurityConfig.java", "modified", 3, 0, 3,
                        "+.requestMatchers(\"/**\").permitAll()\n+csrf.disable()"),
                new ChangedFile("src/main/java/com/example/UserController.java", "modified", 8, 1, 9,
                        "+@PostMapping(\"/users\")\n+public record UserRequest(String name) {}")
        ));

        assertEquals(RiskLevel.HIGH, report.riskLevel());
        assertTrue(report.summary().contains("合并风险"));
        assertTrue(report.items().stream().anyMatch(item -> item.category().equals("依赖/构建")));
        assertTrue(report.items().stream().anyMatch(item -> item.category().equals("安全/权限")));
        assertTrue(report.items().stream().anyMatch(item -> item.category().equals("公开接口")));
        assertTrue(report.items().stream().anyMatch(item -> item.category().equals("测试覆盖")));
    }

    @Test
    void returnsLowRiskForSmallTestOnlyChange() {
        MergeRiskReport report = analyzer.analyze(List.of(
                new ChangedFile("src/test/java/com/example/AppTest.java", "modified", 1, 1, 2,
                        "+assertEquals(1, result)")
        ));

        assertEquals(RiskLevel.LOW, report.riskLevel());
        assertTrue(report.items().isEmpty());
    }

    @Test
    void detectsLargeChangeAndCiRisk() {
        MergeRiskReport report = analyzer.analyze(List.of(
                new ChangedFile(".github/workflows/build.yml", "modified", 20, 5, 25,
                        "+mvn package"),
                new ChangedFile("src/main/java/com/example/BigService.java", "modified", 600, 0, 600,
                        "+".repeat(600))
        ));

        assertEquals(RiskLevel.MEDIUM, report.riskLevel());
        assertTrue(report.items().stream().anyMatch(item -> item.category().equals("CI/部署")));
        assertTrue(report.items().stream().anyMatch(item -> item.category().equals("变更规模")));
    }
}
