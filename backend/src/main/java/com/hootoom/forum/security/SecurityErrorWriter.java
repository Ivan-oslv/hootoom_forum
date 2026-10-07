package com.hootoom.forum.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hootoom.forum.common.api.ErrorResponse;
import com.hootoom.forum.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 将 Security Filter 中的 401/403 也转换为项目统一错误结构。 */
@Component
public class SecurityErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, 401, "UNAUTHENTICATED", "请先登录或重新登录");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException accessDeniedException) throws IOException {
        write(response, 403, "FORBIDDEN", "没有执行该操作的权限");
    }

    private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(code, message, List.of(), MDC.get(RequestIdFilter.MDC_REQUEST_ID)));
    }
}
