package com.hootoom.forum.security;

import com.hootoom.forum.common.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/** 对使用 Refresh Cookie 的状态变更请求执行可信来源和双提交 CSRF 校验。 */
@Component
public class RefreshRequestGuard {
    public static final String REFRESH_COOKIE = "HOOTOOM_REFRESH";
    public static final String CSRF_COOKIE = "HOOTOOM_XSRF";
    public static final String CSRF_HEADER = "X-CSRF-Token";
    private final Set<String> trustedOrigins;
    private final ConcurrentHashMap<String, Window> refreshWindows = new ConcurrentHashMap<>();

    public RefreshRequestGuard(@Value("${app.auth.trusted-origins}") String origins) {
        this.trustedOrigins = Arrays.stream(origins.split(",")).map(String::strip)
                .filter(value -> !value.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    public void verify(HttpServletRequest request) {
        consumeRefreshQuota(request.getRemoteAddr());
        String source = request.getHeader("Origin");
        if (source == null || source.isBlank()) {
            source = originOf(request.getHeader("Referer"));
        }
        if (source == null || !trustedOrigins.contains(source)) {
            throw new BusinessException("FORBIDDEN", "请求来源不受信任", HttpStatus.FORBIDDEN);
        }
        String header = request.getHeader(CSRF_HEADER);
        String cookie = cookie(request, CSRF_COOKIE);
        if (header == null || cookie == null || !MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8), cookie.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("FORBIDDEN", "CSRF校验失败", HttpStatus.FORBIDDEN);
        }
    }

    private void consumeRefreshQuota(String clientAddress) {
        Instant now = Instant.now();
        Window value = refreshWindows.compute(clientAddress, (key, current) -> current == null
                || Duration.between(current.startedAt(), now).toMinutes() >= 1
                ? new Window(now, 1) : new Window(current.startedAt(), current.count() + 1));
        if (value.count() > 30) {
            throw new BusinessException("RATE_LIMITED", "请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public String refreshToken(HttpServletRequest request) {
        return cookie(request, REFRESH_COOKIE);
    }

    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies()).filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }

    private String originOf(String referer) {
        if (referer == null || referer.isBlank()) return null;
        try {
            URI uri = URI.create(referer);
            int port = uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + (port < 0 ? "" : ":" + port);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record Window(Instant startedAt, int count) { }
}
