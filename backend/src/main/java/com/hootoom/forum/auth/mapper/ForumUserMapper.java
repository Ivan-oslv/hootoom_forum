package com.hootoom.forum.auth.mapper;

import com.hootoom.forum.auth.entity.ForumUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ForumUserMapper {
    boolean existsByUsername(@Param("username") String username);
    boolean existsByEmail(@Param("email") String email);
    int insert(ForumUser user);
    int activatePendingUser(@Param("userId") Long userId);
    ForumUser findByIdentifier(@Param("identifier") String identifier);
    ForumUser findById(@Param("id") Long id);
    ForumUser findByEmail(@Param("email") String email);
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") java.time.LocalDateTime lastLoginAt);
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash,
                       @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
