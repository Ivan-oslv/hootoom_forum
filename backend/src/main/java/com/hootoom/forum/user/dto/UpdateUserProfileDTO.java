package com.hootoom.forum.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileDTO(
        @NotBlank(message = "昵称不能为空") @Size(max = 40, message = "昵称不能超过40个字符") String nickname,
        @Size(max = 500, message = "个人简介不能超过500个字符") String bio,
        @NotBlank(message = "头像不能为空")
        @Pattern(regexp = "^[a-z0-9-]{1,64}$", message = "头像标识格式不正确") String avatarKey,
        @NotNull(message = "版本号不能为空") @PositiveOrZero(message = "版本号不能小于0") Integer version
) { }
