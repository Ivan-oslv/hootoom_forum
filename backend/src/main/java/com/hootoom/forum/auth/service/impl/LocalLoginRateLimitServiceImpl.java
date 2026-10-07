package com.hootoom.forum.auth.service.impl;

import com.hootoom.forum.auth.service.LoginRateLimitService;
import com.hootoom.forum.common.exception.BusinessException;
import com.hootoom.forum.security.SecureTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalLoginRateLimitServiceImpl implements LoginRateLimitService {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final SecureTokenService tokenService;

    public LocalLoginRateLimitServiceImpl(SecureTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void checkAndConsume(String clientAddress, String identifier) {
        consume("ip:" + clientAddress, 10, Duration.ofMinutes(1));
        // 内存键也不保留原始邮箱或用户名，便于诊断转储时维持数据最小化。
        consume("account:" + tokenService.sha256(identifier), 10, Duration.ofMinutes(15));
    }

    @Override
    public void resetAccount(String identifier) {
        windows.remove("account:" + tokenService.sha256(identifier));
    }

    private void consume(String key, int limit, Duration duration) {
        Instant now = Instant.now();
        Window result = windows.compute(key, (ignored, current) -> current == null
                || Duration.between(current.startedAt(), now).compareTo(duration) >= 0
                ? new Window(now, 1) : new Window(current.startedAt(), current.count() + 1));
        if (result.count() > limit) {
            throw new BusinessException("RATE_LIMITED", "请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private record Window(Instant startedAt, int count) { }
}
