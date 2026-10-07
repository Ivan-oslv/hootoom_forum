package com.hootoom.forum.user.service.impl;

import com.hootoom.forum.auth.entity.ForumUser;
import com.hootoom.forum.common.exception.BusinessException;
import com.hootoom.forum.governance.service.SensitiveTextService;
import com.hootoom.forum.user.dto.UpdateUserProfileDTO;
import com.hootoom.forum.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileServiceImplTest {
    private final UserProfileMapper mapper = mock(UserProfileMapper.class);
    private final SensitiveTextService sensitiveTextService = mock(SensitiveTextService.class);
    private final UserProfileServiceImpl service = new UserProfileServiceImpl(
            mapper, sensitiveTextService, "default-1,default-2");

    @Test
    void updateUsesOptimisticVersionAndReturnsFreshProfile() {
        ForumUser before = user(2);
        ForumUser after = user(3);
        after.setNickname("新昵称");
        when(mapper.findById(10L)).thenReturn(before, after);
        when(mapper.updateProfile(10L, "新昵称", "个人简介", "default-2", 2)).thenReturn(1);

        var result = service.updateCurrentUser(10L,
                new UpdateUserProfileDTO(" 新昵称 ", " 个人简介 ", "default-2", 2));

        verify(sensitiveTextService).rejectUnsafeProfileText("新昵称", "个人简介");
        assertThat(result.version()).isEqualTo(3);
        assertThat(result.nickname()).isEqualTo("新昵称");
    }

    @Test
    void updateRejectsUnknownAvatarBeforeWriting() {
        when(mapper.findById(10L)).thenReturn(user(2));

        assertThatThrownBy(() -> service.updateCurrentUser(10L,
                new UpdateUserProfileDTO("昵称", null, "remote-avatar", 2)))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("INVALID_REQUEST");
    }

    private ForumUser user(int version) {
        ForumUser user = new ForumUser();
        user.setId(10L); user.setUsername("demo"); user.setEmail("demo@example.com");
        user.setNickname("Demo"); user.setAvatarKey("default-1");
        user.setStatus("ACTIVE"); user.setVersion(version);
        return user;
    }
}
