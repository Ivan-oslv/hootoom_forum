package com.hootoom.forum.auth.vo;

import java.time.Instant;

/** 设备会话视图不暴露 Token 哈希、会话族标识和撤销内部原因。 */
public record SessionVO(String id, String deviceName, Instant createdAt,
                        Instant lastUsedAt, Instant expiresAt) { }
