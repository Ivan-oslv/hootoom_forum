package com.hootoom.forum.auth.mapper;

import com.hootoom.forum.auth.entity.MailOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MailOutboxMapper {
    int insert(MailOutbox mail);
    List<MailOutbox> findDispatchableForUpdate(@Param("now") LocalDateTime now,
                                                @Param("lockExpiredBefore") LocalDateTime lockExpiredBefore,
                                                @Param("limit") int limit);
    int markClaimed(@Param("ids") List<Long> ids, @Param("lockedBy") String lockedBy,
                    @Param("lockedAt") LocalDateTime lockedAt);
    int markSent(@Param("id") Long id, @Param("lockedBy") String lockedBy,
                 @Param("sentAt") LocalDateTime sentAt);
    int markRetry(@Param("id") Long id, @Param("lockedBy") String lockedBy,
                  @Param("status") String status, @Param("retryCount") int retryCount,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt,
                  @Param("errorCode") String errorCode);
}
