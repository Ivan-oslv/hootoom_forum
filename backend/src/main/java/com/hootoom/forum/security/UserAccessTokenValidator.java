package com.hootoom.forum.security;

import com.hootoom.forum.auth.entity.ForumUser;
import com.hootoom.forum.auth.mapper.ForumUserMapper;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** 每次受保护请求校验用户状态与 tokenVersion，使禁用和改密立即使现有 Access Token 失效。 */
@Component
public class UserAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID = new OAuth2Error("invalid_token", "Token subject is inactive", null);
    private final ForumUserMapper userMapper;

    public UserAccessTokenValidator(ForumUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (!"USER".equals(jwt.getClaimAsString("subjectType"))) {
            return OAuth2TokenValidatorResult.failure(INVALID);
        }
        try {
            ForumUser user = userMapper.findById(Long.valueOf(jwt.getSubject()));
            Number version = jwt.getClaim("tokenVersion");
            if (user == null || version == null || !"ACTIVE".equals(user.getStatus())
                    || user.getTokenVersion().intValue() != version.intValue()) {
                return OAuth2TokenValidatorResult.failure(INVALID);
            }
            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID);
        }
    }
}
