package com.hootoom.forum.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 前台用户持久化实体，只在 Mapper 与 ServiceImpl 之间使用。 */
@Data
public class ForumUser {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime emailVerifiedAt;
    private String passwordHash;
    private Integer tokenVersion;
    private String nickname;
    private String avatarKey;
    private String bio;
    private String status;
    private String policyVersion;
    private LocalDateTime policyAcceptedAt;
    private Integer approvedPostCount;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
