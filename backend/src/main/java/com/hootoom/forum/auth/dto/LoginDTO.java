package com.hootoom.forum.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(
        @NotBlank(message = "用户名或邮箱不能为空") @Size(max = 254) String identifier,
        @NotBlank(message = "密码不能为空") @Size(max = 72) String password,
        @Size(max = 100, message = "设备名称不能超过100个字符") String deviceName
) {
}
