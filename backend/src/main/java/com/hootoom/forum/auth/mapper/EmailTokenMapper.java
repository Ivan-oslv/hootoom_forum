package com.hootoom.forum.auth.mapper;

import com.hootoom.forum.auth.entity.EmailToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface EmailTokenMapper {
    int invalidateActiveTokens(@Param("userId") Long userId, @Param("purpose") String purpose,
                               @Param("usedAt") LocalDateTime usedAt);
    int insert(EmailToken token);
    EmailToken findByHashForUpdate(@Param("tokenHash") String tokenHash, @Param("purpose") String purpose);
    int markUsed(@Param("id") Long id, @Param("usedAt") LocalDateTime usedAt);
}
