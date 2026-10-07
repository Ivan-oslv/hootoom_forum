package com.hootoom.forum.common.api;

import com.hootoom.forum.common.web.RequestIdFilter;
import org.slf4j.MDC;

public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "success", data, currentRequestId());
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, currentRequestId());
    }

    private static String currentRequestId() {
        return MDC.get(RequestIdFilter.MDC_REQUEST_ID);
    }
}
