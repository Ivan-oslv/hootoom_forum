package com.hootoom.forum.auth.service;

public interface LoginRateLimitService {
    void checkAndConsume(String clientAddress, String normalizedIdentifier);
    void resetAccount(String normalizedIdentifier);
}
