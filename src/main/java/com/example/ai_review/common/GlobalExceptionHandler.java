package com.example.ai_review.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.upstreamError(exception.getMessage()));
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
                .body(ApiError.upstreamError(message));
    }
}
