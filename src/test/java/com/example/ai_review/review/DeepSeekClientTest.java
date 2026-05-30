package com.example.ai_review.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DeepSeekClient createClient(String apiKey) {
        return new DeepSeekClient(
                RestClient.builder(),
                "https://api.deepseek.com",
                apiKey,
                "deepseek-v4-flash",
                objectMapper
        );
    }

    @Test
    void parsesValidJsonResponse() {
        DeepSeekClient client = createClient("unit-test-key");
        String json = """
                {
                  "summary": "本次 PR 修复了登录 Bug",
                  "riskLevel": "LOW",
                  "risks": [
                    {
                      "file": "App.java",
                      "level": "LOW",
                      "title": "命名不规范",
                      "reason": "变量名 x 不清晰",
                      "suggestion": "改为 userName"
                    }
                  ],
                  "suggestions": [
                    {
                      "file": "App.java",
                      "category": "可维护性",
                      "content": "建议添加单元测试"
                    }
                  ]
                }
                """;

        ReviewReport report = client.parseReviewReport(json);

        assertEquals("本次 PR 修复了登录 Bug", report.summary());
        assertEquals(RiskLevel.LOW, report.riskLevel());
        assertEquals(1, report.risks().size());
        assertEquals("App.java", report.risks().get(0).file());
        assertEquals("LOW", report.risks().get(0).level());
        assertEquals("命名不规范", report.risks().get(0).title());
        assertEquals("变量名 x 不清晰", report.risks().get(0).reason());
        assertEquals("改为 userName", report.risks().get(0).suggestion());
        // 旧格式不含新增字段，应为 null
        assertNull(report.risks().get(0).lineNumber());
        assertNull(report.risks().get(0).codeSnippet());
        assertNull(report.risks().get(0).exampleFix());
        assertEquals(1, report.suggestions().size());
        assertEquals("App.java", report.suggestions().get(0).file());
        assertEquals("可维护性", report.suggestions().get(0).category());
        assertEquals("建议添加单元测试", report.suggestions().get(0).content());
    }

    @Test
    void parsesNewFormatWithEvidence() {
        DeepSeekClient client = createClient("unit-test-key");
        String json = """
                {
                  "summary": "存在空指针风险",
                  "riskLevel": "HIGH",
                  "risks": [
                    {
                      "file": "Service.java",
                      "level": "HIGH",
                      "title": "空指针",
                      "reason": "未检查 null",
                      "suggestion": "加 if 判断",
                      "lineNumber": 42,
                      "codeSnippet": "result.process();",
                      "exampleFix": "if (result != null) { result.process(); }"
                    }
                  ],
                  "suggestions": [
                    {
                      "file": "Service.java",
                      "category": "性能",
                      "content": "使用 StringBuilder",
                      "lineNumber": 15,
                      "codeSnippet": "String s = \\"\\";",
                      "exampleFix": "StringBuilder sb = new StringBuilder();"
                    }
                  ]
                }
                """;

        ReviewReport report = client.parseReviewReport(json);

        assertEquals("存在空指针风险", report.summary());
        assertEquals(RiskLevel.HIGH, report.riskLevel());
        assertEquals(1, report.risks().size());
        RiskItem risk = report.risks().get(0);
        assertEquals(42, risk.lineNumber());
        assertEquals("result.process();", risk.codeSnippet());
        assertEquals("if (result != null) { result.process(); }", risk.exampleFix());
        assertEquals(1, report.suggestions().size());
        SuggestionItem sug = report.suggestions().get(0);
        assertEquals(15, sug.lineNumber());
        assertEquals("String s = \"\";", sug.codeSnippet());
        assertEquals("StringBuilder sb = new StringBuilder();", sug.exampleFix());
    }

    @Test
    void parsesEmptyRisksResponse() {
        DeepSeekClient client = createClient("unit-test-key");
        String json = """
                {
                  "summary": "未发现明显风险，代码质量良好",
                  "riskLevel": "LOW",
                  "risks": [],
                  "suggestions": []
                }
                """;

        ReviewReport report = client.parseReviewReport(json);

        assertEquals("未发现明显风险，代码质量良好", report.summary());
        assertEquals(RiskLevel.LOW, report.riskLevel());
        assertTrue(report.risks().isEmpty());
        assertTrue(report.suggestions().isEmpty());
    }

    @Test
    void throwsOnInvalidJson() {
        DeepSeekClient client = createClient("unit-test-key");

        assertThrows(DeepSeekApiException.class, () ->
                client.parseReviewReport("这不是 JSON"));
    }

    @Test
    void throwsOnEmptyApiKey() {
        DeepSeekClient client = createClient("");

        DeepSeekApiException ex = assertThrows(DeepSeekApiException.class, () ->
                client.chat("system", "user"));
        assertTrue(ex.getMessage().contains("DEEPSEEK_API_KEY"));
    }

    @Test
    void serializesMaxTokensAsSnakeCase() throws Exception {
        DeepSeekChatRequest request = new DeepSeekChatRequest(
                "deepseek-v4-flash",
                List.of(new DeepSeekChatRequest.Message("user", "hello")),
                0.1,
                4096,
                new DeepSeekChatRequest.ResponseFormat("json_object")
        );

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"max_tokens\""));
        assertFalse(json.contains("\"maxTokens\""));
    }

    @Test
    void throwsOnMissingSummary() {
        DeepSeekClient client = createClient("unit-test-key");
        String json = """
                {
                  "riskLevel": "LOW",
                  "risks": [],
                  "suggestions": []
                }
                """;

        DeepSeekApiException ex = assertThrows(DeepSeekApiException.class, () ->
                client.parseReviewReport(json));
        assertTrue(ex.getMessage().contains("summary"));
    }

    @Test
    void throwsOnMissingRiskLevel() {
        DeepSeekClient client = createClient("unit-test-key");
        String json = """
                {
                  "summary": "PR 总结",
                  "risks": [],
                  "suggestions": []
                }
                """;

        DeepSeekApiException ex = assertThrows(DeepSeekApiException.class, () ->
                client.parseReviewReport(json));
        assertTrue(ex.getMessage().contains("riskLevel"));
    }

    @Test
    void throwsOnMissingRisks() {
        DeepSeekClient client = createClient("unit-test-key");
        String json = """
                {
                  "summary": "PR 总结",
                  "riskLevel": "LOW",
                  "suggestions": []
                }
                """;

        DeepSeekApiException ex = assertThrows(DeepSeekApiException.class, () ->
                client.parseReviewReport(json));
        assertTrue(ex.getMessage().contains("risks"));
    }

    @Test
    void throwsOnMissingSuggestions() {
        DeepSeekClient client = createClient("unit-test-key");
        String json = """
                {
                  "summary": "PR 总结",
                  "riskLevel": "LOW",
                  "risks": []
                }
                """;

        DeepSeekApiException ex = assertThrows(DeepSeekApiException.class, () ->
                client.parseReviewReport(json));
        assertTrue(ex.getMessage().contains("suggestions"));
    }

    @Test
    void serializesResponseFormat() throws Exception {
        DeepSeekChatRequest request = new DeepSeekChatRequest(
                "deepseek-v4-flash",
                List.of(new DeepSeekChatRequest.Message("user", "hello")),
                0.1,
                4096,
                new DeepSeekChatRequest.ResponseFormat("json_object")
        );

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"response_format\""));
        assertTrue(json.contains("\"json_object\""));
    }
}
