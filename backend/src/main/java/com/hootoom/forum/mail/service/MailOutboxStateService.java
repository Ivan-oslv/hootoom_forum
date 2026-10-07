package com.hootoom.forum.mail.service;

import java.time.LocalDateTime;

/** 独立 Bean 保证投递结果更新经过 Spring 事务代理，避免同类调用使事务注解失效。 */
public interface MailOutboxStateService {
    void markSent(Long id, String workerId, LocalDateTime sentAt);
    void markFailed(Long id, String workerId, String status, int retryCount,
                    LocalDateTime nextRetryAt, String errorCode);
}
