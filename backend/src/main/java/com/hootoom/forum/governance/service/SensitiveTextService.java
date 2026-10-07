package com.hootoom.forum.governance.service;

public interface SensitiveTextService {
    void rejectUnsafeProfileText(String nickname, String bio);
}
