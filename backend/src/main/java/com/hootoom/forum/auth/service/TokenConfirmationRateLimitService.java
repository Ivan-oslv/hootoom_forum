package com.hootoom.forum.auth.service;

public interface TokenConfirmationRateLimitService {
    void checkAndConsume(String clientAddress);
}
