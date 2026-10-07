package com.hootoom.forum.user.vo;

import java.time.Instant;

public record UserProfileVO(String id, String username, String email, Instant emailVerifiedAt,
                            String nickname, String avatarKey, String bio, int version) { }
