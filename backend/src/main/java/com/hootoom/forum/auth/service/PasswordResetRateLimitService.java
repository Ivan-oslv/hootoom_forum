package com.hootoom.forum.auth.service;

public interface PasswordResetRateLimitService {
    void checkAndConsume(String clientAddress);
}
