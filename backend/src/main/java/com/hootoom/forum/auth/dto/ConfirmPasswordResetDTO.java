package com.hootoom.forum.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmPasswordResetDTO(
        @NotBlank(message = "重置令牌不能为空") @Size(max = 200) String token,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 10, max = 72, message = "密码长度必须为10到72个字符")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String newPassword
) { }
