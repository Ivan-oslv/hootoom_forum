package com.hootoom.forum.user.service.impl;

import com.hootoom.forum.auth.entity.ForumUser;
import com.hootoom.forum.common.exception.BusinessException;
import com.hootoom.forum.governance.service.SensitiveTextService;
import com.hootoom.forum.user.dto.UpdateUserProfileDTO;
import com.hootoom.forum.user.mapper.UserProfileMapper;
import com.hootoom.forum.user.service.UserProfileService;
import com.hootoom.forum.user.vo.UserProfileVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileMapper mapper;
    private final SensitiveTextService sensitiveTextService;
    private final Set<String> allowedAvatarKeys;

    public UserProfileServiceImpl(UserProfileMapper mapper, SensitiveTextService sensitiveTextService,
                                  @Value("${app.profile.allowed-avatar-keys}") String avatarKeys) {
        this.mapper = mapper;
        this.sensitiveTextService = sensitiveTextService;
        this.allowedAvatarKeys = Arrays.stream(avatarKeys.split(",")).map(String::strip)
                .filter(value -> !value.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileVO getCurrentUser(Long userId) {
        return toView(requireActiveUser(userId));
    }

    @Override
    @Transactional
    public UserProfileVO updateCurrentUser(Long userId, UpdateUserProfileDTO command) {
        requireActiveUser(userId);
        String nickname = command.nickname().strip();
        String bio = command.bio() == null || command.bio().isBlank() ? null : command.bio().strip();
        if (!allowedAvatarKeys.contains(command.avatarKey())) {
            throw new BusinessException("INVALID_REQUEST", "请选择有效的系统头像", HttpStatus.BAD_REQUEST);
        }
        sensitiveTextService.rejectUnsafeProfileText(nickname, bio);
        if (mapper.updateProfile(userId, nickname, bio, command.avatarKey(), command.version()) != 1) {
            throw new BusinessException("RESOURCE_CONFLICT", "资料已被更新，请刷新后重试", HttpStatus.CONFLICT);
        }
        return toView(requireActiveUser(userId));
    }

    private ForumUser requireActiveUser(Long userId) {
        ForumUser user = mapper.findById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("RESOURCE_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND);
        }
        return user;
    }

    private UserProfileVO toView(ForumUser user) {
        return new UserProfileVO(String.valueOf(user.getId()), user.getUsername(), user.getEmail(),
                user.getEmailVerifiedAt() == null ? null : user.getEmailVerifiedAt().toInstant(ZoneOffset.UTC),
                user.getNickname(), user.getAvatarKey(), user.getBio(), user.getVersion());
    }
}
