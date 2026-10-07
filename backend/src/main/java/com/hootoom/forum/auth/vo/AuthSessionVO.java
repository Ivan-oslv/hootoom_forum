package com.hootoom.forum.auth.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** 对外仅序列化 Access Token；Refresh Token 与 CSRF 值由 Controller 写入安全 Cookie。 */
public record AuthSessionVO(
        String accessToken,
        long expiresIn,
        CurrentUserVO user,
        @JsonIgnore String refreshToken,
        @JsonIgnore String csrfToken
) {
}
