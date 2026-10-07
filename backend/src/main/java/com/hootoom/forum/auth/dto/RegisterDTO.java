package com.hootoom.forum.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度必须为3到32个字符")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱长度不能超过254个字符")
        String email,
        @NotBlank(message = "密码不能为空")
        @Size(min = 10, max = 72, message = "密码长度必须为10到72个字符")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password,
        @NotBlank(message = "协议版本不能为空") String policyVersion,
        @AssertTrue(message = "必须同意用户协议和隐私政策") boolean policyAccepted
) {
}
