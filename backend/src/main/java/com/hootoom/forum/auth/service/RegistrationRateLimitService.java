package com.hootoom.forum.auth.service;

/** 注册入口的单实例限流能力；多实例生产环境必须替换为网关或 Redis 统一限流。 */
public interface RegistrationRateLimitService {
    void checkAndConsume(String clientAddress);
}
