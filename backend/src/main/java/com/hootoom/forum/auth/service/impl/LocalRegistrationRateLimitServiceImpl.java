package com.hootoom.forum.auth.service.impl;

import com.hootoom.forum.auth.service.RegistrationRateLimitService;
import com.hootoom.forum.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalRegistrationRateLimitServiceImpl implements RegistrationRateLimitService {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final Clock clock;

    // 明确标记生产构造器；带 Clock 的构造器仅供窗口边界测试使用。
    @Autowired
    public LocalRegistrationRateLimitServiceImpl(
            @Value("${app.auth.registration-limit-per-hour:5}") int limit) {
        this(limit, Clock.systemUTC());
    }

    LocalRegistrationRateLimitServiceImpl(int limit, Clock clock) {
        this.limit = limit;
        this.clock = clock;
    }

    @Override
    public void checkAndConsume(String clientAddress) {
        Instant now = clock.instant();
        Window updated = windows.compute(clientAddress, (key, current) -> {
            if (current == null || Duration.between(current.startedAt(), now).toHours() >= 1) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });
        if (updated.count() > limit) {
            // 不输出地址和内部桶状态，避免日志和响应泄露风控细节。
            throw new BusinessException("RATE_LIMITED", "请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private record Window(Instant startedAt, int count) {
    }
}
