package com.hootoom.forum.user.service;

import com.hootoom.forum.user.dto.UpdateUserProfileDTO;
import com.hootoom.forum.user.vo.UserProfileVO;

public interface UserProfileService {
    UserProfileVO getCurrentUser(Long userId);
    UserProfileVO updateCurrentUser(Long userId, UpdateUserProfileDTO command);
}
