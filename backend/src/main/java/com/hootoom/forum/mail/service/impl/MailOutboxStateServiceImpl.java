package com.hootoom.forum.mail.service.impl;

import com.hootoom.forum.auth.mapper.MailOutboxMapper;
import com.hootoom.forum.mail.service.MailOutboxStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MailOutboxStateServiceImpl implements MailOutboxStateService {
    private final MailOutboxMapper mapper;

    public MailOutboxStateServiceImpl(MailOutboxMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void markSent(Long id, String workerId, LocalDateTime sentAt) {
        mapper.markSent(id, workerId, sentAt);
    }

    @Override
    @Transactional
    public void markFailed(Long id, String workerId, String status, int retryCount,
                           LocalDateTime nextRetryAt, String errorCode) {
        mapper.markRetry(id, workerId, status, retryCount, nextRetryAt, errorCode);
    }
}
