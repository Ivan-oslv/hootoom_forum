package com.hootoom.forum.auth.vo;

/** 注册响应不返回邮箱、密码哈希或一次性令牌。 */
public record RegistrationVO(String userId, String status, String message) {
}
