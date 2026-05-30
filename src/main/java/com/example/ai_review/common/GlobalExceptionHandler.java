package com.example.ai_review.common;

import com.example.ai_review.review.DeepSeekApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(exception.getCode().name(), exception.getMessage(),
                        exception.getSuggestion(), false, java.time.Instant.now().toString()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.badRequest(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数不合法");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.badRequest(message));
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ApiError> handleGitHubApi(GitHubApiException exception) {
        ErrorCode code = exception.getCode();
        HttpStatus status;
        if (code == ErrorCode.GITHUB_PR_NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        } else if (code == ErrorCode.GITHUB_AUTH_FAILED || code == ErrorCode.GITHUB_TOKEN_REQUIRED) {
            status = HttpStatus.UNAUTHORIZED;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }
        return ResponseEntity.status(status)
                .body(ApiError.upstreamError(code, exception.getMessage(),
                        exception.getSuggestion(), exception.isRetryable()));
    }

    @ExceptionHandler(DeepSeekApiException.class)
    public ResponseEntity<ApiError> handleDeepSeekApi(DeepSeekApiException exception) {
        ErrorCode code = exception.getCode();
        HttpStatus status;
        if (code == ErrorCode.DEEPSEEK_AUTH_FAILED || code == ErrorCode.DEEPSEEK_API_KEY_MISSING) {
            status = HttpStatus.UNAUTHORIZED;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }
        return ResponseEntity.status(status)
                .body(ApiError.upstreamError(code, exception.getMessage(),
                        exception.getSuggestion(), exception.isRetryable()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handleRestClient(RestClientException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("Connection refused")) {
            message = "无法连接到 GitHub API，请检查网络连接";
        } else if (message != null && message.contains("UnknownHostException")) {
            message = "无法解析 GitHub API 域名，请检查 DNS 或网络设置";
        } else {
            message = "调用 GitHub API 时发生网络错误，请稍后重试";
        }
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.upstreamError(ErrorCode.GITHUB_NETWORK_ERROR, message,
                        "检查网络连接；如果只是提交前自查，可切换到本地 Diff Review", true));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.upstreamError(ErrorCode.INTERNAL_ERROR,
                        "服务器内部错误，请稍后重试", "如持续出现请联系维护者", true));
    }
}
