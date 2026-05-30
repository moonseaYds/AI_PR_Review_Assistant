package com.example.ai_review.review;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeepSeekClient deepSeekClient;

    @Test
    void aiReviewReturnsStructuredReport() throws Exception {
        when(deepSeekClient.chat(any(), any())).thenReturn("any json");
        when(deepSeekClient.parseReviewReport(any())).thenReturn(new ReviewReport(
                "本次 PR 修复了登录 Bug，代码质量良好",
                RiskLevel.LOW,
                List.of(new RiskItem("App.java", "LOW", "命名建议", "变量 x 含义不明确", "改为 userLoginCount", null, null, null)),
                List.of(new SuggestionItem("App.java", "可维护性", "建议添加单元测试", null, null, null)),
                "deepseek-v4-flash"
        ));
        when(deepSeekClient.getModel()).thenReturn("deepseek-v4-flash");

        mockMvc.perform(post("/api/reviews/ai-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "test-owner",
                                  "repo": "test-repo",
                                  "pullNumber": 1,
                                  "title": "Fix login bug",
                                  "totalFiles": 1,
                                  "totalAdditions": 10,
                                  "totalDeletions": 3,
                                  "totalChanges": 13,
                                  "truncated": false,
                                  "truncationReason": null,
                                  "fileContexts": [
                                    {
                                      "filename": "App.java",
                                      "status": "modified",
                                      "additions": 10,
                                      "deletions": 3,
                                      "changes": 13,
                                      "patchExcerpt": "@@ -1,3 +1,10 @@ ...",
                                      "patchTruncated": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("本次 PR 修复了登录 Bug，代码质量良好"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.risks[0].file").value("App.java"))
                .andExpect(jsonPath("$.risks[0].level").value("LOW"))
                .andExpect(jsonPath("$.risks[0].title").value("命名建议"))
                .andExpect(jsonPath("$.suggestions[0].file").value("App.java"))
                .andExpect(jsonPath("$.suggestions[0].category").value("可维护性"))
                .andExpect(jsonPath("$.model").value("deepseek-v4-flash"));
    }

    @Test
    void aiReviewReturnsEmptyRisksForCleanCode() throws Exception {
        when(deepSeekClient.chat(any(), any())).thenReturn("any json");
        when(deepSeekClient.parseReviewReport(any())).thenReturn(new ReviewReport(
                "未发现明显风险，代码简洁清晰",
                RiskLevel.LOW,
                List.of(),
                List.of(),
                "deepseek-v4-flash"
        ));
        when(deepSeekClient.getModel()).thenReturn("deepseek-v4-flash");

        mockMvc.perform(post("/api/reviews/ai-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "o",
                                  "repo": "r",
                                  "pullNumber": 2,
                                  "title": "Clean PR",
                                  "totalFiles": 1,
                                  "totalAdditions": 5,
                                  "totalDeletions": 1,
                                  "totalChanges": 6,
                                  "truncated": false,
                                  "truncationReason": null,
                                  "fileContexts": [
                                    {
                                      "filename": "Clean.java",
                                      "status": "modified",
                                      "additions": 5,
                                      "deletions": 1,
                                      "changes": 6,
                                      "patchExcerpt": "@@ -1 +1 @@ clean",
                                      "patchTruncated": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("未发现明显风险，代码简洁清晰"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.risks").isArray())
                .andExpect(jsonPath("$.risks").isEmpty())
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.suggestions").isEmpty());
    }

    @Test
    void aiReviewReturnsHighRiskForBugPr() throws Exception {
        when(deepSeekClient.chat(any(), any())).thenReturn("any json");
        when(deepSeekClient.parseReviewReport(any())).thenReturn(new ReviewReport(
                "本次 PR 存在严重空指针风险",
                RiskLevel.HIGH,
                List.of(new RiskItem("Service.java", "HIGH", "空指针风险",
                        "第15行未对返回值做 null 检查", "添加 if (result != null) 判断", null, null, null)),
                List.of(),
                "deepseek-v4-flash"
        ));
        when(deepSeekClient.getModel()).thenReturn("deepseek-v4-flash");

        mockMvc.perform(post("/api/reviews/ai-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "o",
                                  "repo": "r",
                                  "pullNumber": 3,
                                  "title": "Risky PR",
                                  "totalFiles": 1,
                                  "totalAdditions": 1,
                                  "totalDeletions": 0,
                                  "totalChanges": 1,
                                  "truncated": false,
                                  "truncationReason": null,
                                  "fileContexts": [
                                    {
                                      "filename": "Service.java",
                                      "status": "modified",
                                      "additions": 1,
                                      "deletions": 0,
                                      "changes": 1,
                                      "patchExcerpt": "+ result.process();",
                                      "patchTruncated": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.risks[0].level").value("HIGH"))
                .andExpect(jsonPath("$.risks[0].title").value("空指针风险"));
    }

    @Test
    void aiReviewReturns400ForEmptyFileContexts() throws Exception {
        mockMvc.perform(post("/api/reviews/ai-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "o",
                                  "repo": "r",
                                  "pullNumber": 1,
                                  "title": "Empty",
                                  "totalFiles": 0,
                                  "totalAdditions": 0,
                                  "totalDeletions": 0,
                                  "totalChanges": 0,
                                  "truncated": false,
                                  "truncationReason": null,
                                  "fileContexts": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("不能为空")));
    }

    @Test
    void aiReviewReturns502WhenApiKeyMissing() throws Exception {
        when(deepSeekClient.chat(any(), any()))
                .thenThrow(new DeepSeekApiException(
                        "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY"));
        when(deepSeekClient.getModel()).thenReturn("deepseek-v4-flash");

        mockMvc.perform(post("/api/reviews/ai-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "o",
                                  "repo": "r",
                                  "pullNumber": 1,
                                  "title": "Test",
                                  "totalFiles": 1,
                                  "totalAdditions": 1,
                                  "totalDeletions": 0,
                                  "totalChanges": 1,
                                  "truncated": false,
                                  "truncationReason": null,
                                  "fileContexts": [
                                    {
                                      "filename": "App.java",
                                      "status": "modified",
                                      "additions": 1,
                                      "deletions": 0,
                                      "changes": 1,
                                      "patchExcerpt": "+ code",
                                      "patchTruncated": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("DEEPSEEK_UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message", containsString("DEEPSEEK_API_KEY")));
    }

    @Test
    void aiReviewReturns502WhenParseFails() throws Exception {
        when(deepSeekClient.chat(any(), any())).thenReturn("invalid response");
        when(deepSeekClient.parseReviewReport(any()))
                .thenThrow(new DeepSeekApiException(
                        "DeepSeek 返回的内容不是合法的 JSON 格式，无法解析为 Review 报告"));
        when(deepSeekClient.getModel()).thenReturn("deepseek-v4-flash");

        mockMvc.perform(post("/api/reviews/ai-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "o",
                                  "repo": "r",
                                  "pullNumber": 1,
                                  "title": "Test",
                                  "totalFiles": 1,
                                  "totalAdditions": 1,
                                  "totalDeletions": 0,
                                  "totalChanges": 1,
                                  "truncated": false,
                                  "truncationReason": null,
                                  "fileContexts": [
                                    {
                                      "filename": "App.java",
                                      "status": "modified",
                                      "additions": 1,
                                      "deletions": 0,
                                      "changes": 1,
                                      "patchExcerpt": "+ code",
                                      "patchTruncated": false
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("DEEPSEEK_UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message", containsString("JSON")));
    }
}
