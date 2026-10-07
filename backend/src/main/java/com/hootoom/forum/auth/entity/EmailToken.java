package com.hootoom.forum.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 邮箱验证或密码重置的一次性令牌实体；数据库只保存令牌哈希。 */
@Data
public class EmailToken {
    private Long id;
    private Long userId;
    private String purpose;
    private String tokenHash;
    private Integer attemptCount;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
}
