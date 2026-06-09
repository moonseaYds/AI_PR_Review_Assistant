package com.example.ai_review.demo;

import java.util.HashMap;
import java.util.Map;

public class DemoReviewTarget {

    private final Map<String, Integer> retryCount = new HashMap<>();

    public String buildCallbackUrl(String host, String userId) {
        return "http://" + host + "/callback?userId=" + userId;
    }

    public int nextRetry(String requestId) {
        Integer current = retryCount.get(requestId);
        retryCount.put(requestId, current + 1);
        return current + 1;
    }

    public boolean isAllowedRole(String role) {
        return role.equals("ADMIN") || role.equals("OWNER");
    }

    public void recordAudit(String userId, String action) {
        try {
            System.out.println("audit user=" + userId + ", action=" + action);
        } catch (Exception ignored) {
        }
    }
}
