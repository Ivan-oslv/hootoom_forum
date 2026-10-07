package com.hootoom.forum.user.mapper;

import com.hootoom.forum.auth.entity.ForumUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper {
    ForumUser findById(@Param("userId") Long userId);
    int updateProfile(@Param("userId") Long userId, @Param("nickname") String nickname,
                      @Param("bio") String bio, @Param("avatarKey") String avatarKey,
                      @Param("version") int version);
}
