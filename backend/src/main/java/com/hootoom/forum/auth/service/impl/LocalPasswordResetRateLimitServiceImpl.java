package com.hootoom.forum.auth.service.impl;

import com.hootoom.forum.auth.service.PasswordResetRateLimitService;
import com.hootoom.forum.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalPasswordResetRateLimitServiceImpl implements PasswordResetRateLimitService {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public void checkAndConsume(String clientAddress) {
        Instant now = Instant.now();
        Window value = windows.compute(clientAddress, (key, current) -> current == null
                || Duration.between(current.startedAt(), now).toHours() >= 1
                ? new Window(now, 1) : new Window(current.startedAt(), current.count() + 1));
        if (value.count() > 5) {
            throw new BusinessException("RATE_LIMITED", "请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private record Window(Instant startedAt, int count) { }
}
