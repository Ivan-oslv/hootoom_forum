package com.hootoom.forum.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** Refresh Token 会话记录；tokenHash 是唯一持久化的令牌表示。 */
@Data
public class RefreshToken {
    private Long id;
    private String subjectType;
    private Long userId;
    private Long adminUserId;
    private String tokenHash;
    private String familyId;
    private String deviceName;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private LocalDateTime createdAt;
}
