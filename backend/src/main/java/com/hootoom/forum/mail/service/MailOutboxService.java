package com.hootoom.forum.mail.service;

import com.hootoom.forum.auth.entity.MailOutbox;

import java.util.List;

public interface MailOutboxService {
    List<MailOutbox> claimBatch(String workerId);
    void deliver(MailOutbox task, String workerId);
}
