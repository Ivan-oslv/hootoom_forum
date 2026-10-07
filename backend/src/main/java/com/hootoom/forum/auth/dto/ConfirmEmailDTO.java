package com.hootoom.forum.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmEmailDTO(
        @NotBlank(message = "验证令牌不能为空")
        @Size(max = 200, message = "验证令牌格式不正确")
        String token
) {
}
