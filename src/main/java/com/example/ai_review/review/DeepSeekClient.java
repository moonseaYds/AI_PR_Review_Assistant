package com.example.ai_review.review;

import com.example.ai_review.common.ErrorCode;
import com.example.ai_review.common.RuntimeCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class DeepSeekClient implements AiReviewModelClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(RestClient.Builder restClientBuilder,
                          @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                          @Value("${deepseek.api-key:}") String apiKey,
                          @Value("${deepseek.model:deepseek-v4-flash}") String model,
                          ObjectMapper objectMapper) {
        this.model = model;
        this.apiKey = (apiKey != null) ? apiKey.strip() : "";
        this.objectMapper = objectMapper;

        String url = (baseUrl != null) ? baseUrl.strip() : "https://api.deepseek.com";
        if (!url.endsWith("/")) {
            url = url + "/";
        }

        String chatUrl = url + "chat/completions";
        this.restClient = restClientBuilder
                .baseUrl(chatUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, RuntimeCredentials.empty());
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, RuntimeCredentials credentials) {
        String effectiveApiKey = effectiveApiKey(credentials);
        if (effectiveApiKey.isEmpty()) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_API_KEY_MISSING,
                    "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY",
                    "可在环境变量、.env 文件中设置 DEEPSEEK_API_KEY，或在 Web Demo 中临时输入 API Key 后重试", false);
        }

        DeepSeekChatRequest request = new DeepSeekChatRequest(
                model,
                List.of(
                        new DeepSeekChatRequest.Message("system", systemPrompt),
                        new DeepSeekChatRequest.Message("user", userPrompt)
                ),
                0.1,
                4096,
                new DeepSeekChatRequest.ResponseFormat("json_object")
        );

        DeepSeekChatResponse response;
        try {
            response = restClient.post()
                    .headers(headers -> headers.set("Authorization", "Bearer " + effectiveApiKey))
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() >= 400, (req, res) -> {
                        byte[] body = res.getBody().readAllBytes();
                        String bodyText = new String(body);
                        if (res.getStatusCode().value() == 401 || res.getStatusCode().value() == 403) {
                            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_AUTH_FAILED,
                                    "DeepSeek API 认证失败，请检查 DEEPSEEK_API_KEY 是否正确",
                                    "检查 key 是否有效、是否有额度、模型是否有权限", false);
                        }
                        throw new DeepSeekApiException(ErrorCode.DEEPSEEK_UPSTREAM_ERROR,
                                "DeepSeek API 返回错误 (" + res.getStatusCode().value() + ")："
                                        + (bodyText.length() > 500 ? bodyText.substring(0, 500) : bodyText),
                                "检查请求参数和模型配置", false);
                    })
                    .body(DeepSeekChatResponse.class);
        } catch (DeepSeekApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_NETWORK_ERROR,
                    "调用 DeepSeek API 时发生网络错误，请检查网络连接和 DEEPSEEK_BASE_URL 配置",
                    "检查网络后重试", true, e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_RESPONSE_INVALID,
                    "DeepSeek API 返回了空的响应内容",
                    "可重试；如果持续出现，尝试降低 diff 大小", true);
        }

        DeepSeekChatResponse.Message message = response.choices().get(0).message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_RESPONSE_INVALID,
                    "DeepSeek API 返回了空的响应内容",
                    "可重试；如果持续出现，尝试降低 diff 大小", true);
        }

        return message.content();
    }

    private String effectiveApiKey(RuntimeCredentials credentials) {
        String runtimeApiKey = credentials != null ? credentials.normalizedDeepSeekApiKey() : "";
        return !runtimeApiKey.isEmpty() ? runtimeApiKey : apiKey;
    }

    public ReviewReport parseReviewReport(String jsonContent) {
        ReviewReport report;
        try {
            report = objectMapper.readValue(jsonContent, ReviewReport.class);
        } catch (JsonProcessingException e) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_RESPONSE_INVALID,
                    "DeepSeek 返回的内容不是合法的 JSON 格式，无法解析为 Review 报告",
                    "可重试；如果持续出现，尝试降低 PR diff 大小或调整模型配置", true, e);
        }
        if (report.summary() == null || report.summary().isBlank()) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_RESPONSE_INVALID,
                    "DeepSeek 返回的 Review 报告缺少 summary 字段或为空",
                    "可重试；如果持续出现，尝试降低 PR diff 大小", true);
        }
        if (report.riskLevel() == null) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_RESPONSE_INVALID,
                    "DeepSeek 返回的 Review 报告缺少 riskLevel 字段",
                    "可重试；如果持续出现，尝试降低 PR diff 大小", true);
        }
        if (report.risks() == null) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_RESPONSE_INVALID,
                    "DeepSeek 返回的 Review 报告缺少 risks 字段",
                    "可重试；如果持续出现，尝试降低 PR diff 大小", true);
        }
        if (report.suggestions() == null) {
            throw new DeepSeekApiException(ErrorCode.DEEPSEEK_RESPONSE_INVALID,
                    "DeepSeek 返回的 Review 报告缺少 suggestions 字段",
                    "可重试；如果持续出现，尝试降低 PR diff 大小", true);
        }
        return report;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String getProviderName() {
        return "deepseek";
    }
}
