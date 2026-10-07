package com.hootoom.forum.auth.mapper;

import com.hootoom.forum.auth.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RefreshTokenMapper {
    int insert(RefreshToken token);
    RefreshToken findByHashForUpdate(@Param("tokenHash") String tokenHash);
    int revokeById(@Param("id") Long id, @Param("revokedAt") LocalDateTime revokedAt,
                   @Param("reason") String reason);
    int revokeFamily(@Param("familyId") String familyId, @Param("revokedAt") LocalDateTime revokedAt,
                     @Param("reason") String reason);
    int revokeAllForUser(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt,
                         @Param("reason") String reason);
    List<RefreshToken> findActiveByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    int revokeOwnedSession(@Param("id") Long id, @Param("userId") Long userId,
                           @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
}
