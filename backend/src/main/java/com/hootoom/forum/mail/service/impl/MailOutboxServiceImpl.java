package com.hootoom.forum.mail.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hootoom.forum.auth.entity.MailOutbox;
import com.hootoom.forum.auth.mapper.MailOutboxMapper;
import com.hootoom.forum.mail.service.MailOutboxService;
import com.hootoom.forum.mail.service.MailOutboxStateService;
import com.hootoom.forum.security.EnvelopeEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class MailOutboxServiceImpl implements MailOutboxService {
    private static final Logger log = LoggerFactory.getLogger(MailOutboxServiceImpl.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final MailOutboxMapper mapper;
    private final EnvelopeEncryptionService encryptionService;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final MailOutboxStateService stateService;
    private final String from;
    private final int batchSize;
    private final int maxRetries;
    private final int lockTimeoutMinutes;
    private final Clock clock;

    // 明确标记生产构造器；带 Clock 的构造器用于测试重试和锁超时逻辑。
    @Autowired
    public MailOutboxServiceImpl(MailOutboxMapper mapper, EnvelopeEncryptionService encryptionService,
                                 JavaMailSender mailSender, ObjectMapper objectMapper,
                                 MailOutboxStateService stateService,
                                 @Value("${app.mail.from:}") String from,
                                 @Value("${app.mail.outbox-batch-size:20}") int batchSize,
                                 @Value("${app.mail.outbox-max-retries:5}") int maxRetries,
                                 @Value("${app.mail.outbox-lock-timeout-minutes:10}") int lockTimeoutMinutes) {
        this(mapper, encryptionService, mailSender, objectMapper, stateService, from, batchSize, maxRetries,
                lockTimeoutMinutes, Clock.systemUTC());
    }

    MailOutboxServiceImpl(MailOutboxMapper mapper, EnvelopeEncryptionService encryptionService,
                          JavaMailSender mailSender, ObjectMapper objectMapper,
                          MailOutboxStateService stateService, String from,
                          int batchSize, int maxRetries, int lockTimeoutMinutes, Clock clock) {
        this.mapper = mapper;
        this.encryptionService = encryptionService;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.stateService = stateService;
        this.from = from;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.lockTimeoutMinutes = lockTimeoutMinutes;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<MailOutbox> claimBatch(String workerId) {
        LocalDateTime now = now();
        List<MailOutbox> tasks = mapper.findDispatchableForUpdate(
                now, now.minusMinutes(lockTimeoutMinutes), batchSize);
        if (!tasks.isEmpty()) {
            mapper.markClaimed(tasks.stream().map(MailOutbox::getId).toList(), workerId, now);
        }
        return tasks;
    }

    @Override
    public void deliver(MailOutbox task, String workerId) {
        try {
            String recipient = encryptionService.decrypt(task.getRecipientCiphertext(), task.getEncryptionKeyVersion());
            String payloadJson = encryptionService.decrypt(task.getPayloadCiphertext(), task.getEncryptionKeyVersion());
            Map<String, Object> payload = objectMapper.readValue(payloadJson, MAP_TYPE);
            send(task.getTemplateCode(), recipient, payload);
            stateService.markSent(task.getId(), workerId, now());
            log.info("mail outbox delivered: outboxId={}, templateCode={}, keyVersion={}, status=SENT, retryCount={}",
                    task.getId(), task.getTemplateCode(), task.getEncryptionKeyVersion(), task.getRetryCount());
        } catch (Exception exception) {
            handleFailure(task, workerId, exception);
        }
    }

    private void send(String templateCode, String recipient, Map<String, Object> payload) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(recipient);
        if ("VERIFY_EMAIL".equals(templateCode)) {
            message.setSubject("HOOTOOM Forum 邮箱验证");
            message.setText("请在 " + payload.get("expiresInMinutes") + " 分钟内完成邮箱验证：\n"
                    + payload.get("verificationUrl") + "\n\n如果不是你本人操作，请忽略此邮件。");
        } else if ("RESET_PASSWORD".equals(templateCode)) {
            message.setSubject("HOOTOOM Forum 密码重置");
            message.setText("请在 " + payload.get("expiresInMinutes") + " 分钟内重置密码：\n"
                    + payload.get("resetUrl") + "\n\n如果不是你本人操作，请忽略此邮件。");
        } else {
            throw new IllegalArgumentException("Unsupported mail template: " + templateCode);
        }
        mailSender.send(message);
    }

    private void handleFailure(MailOutbox task, String workerId, Exception exception) {
        int retryCount = task.getRetryCount() + 1;
        boolean dead = retryCount >= maxRetries;
        String status = dead ? "DEAD" : "RETRY";
        // 指数退避以分钟为单位，并设置60分钟上限，防止故障期间形成高频重试风暴。
        long delayMinutes = Math.min(60, 1L << Math.min(retryCount - 1, 6));
        String errorCode = exception.getClass().getSimpleName();
        stateService.markFailed(task.getId(), workerId, status, retryCount,
                now().plusMinutes(delayMinutes), errorCode);
        if (dead) {
            log.error("mail outbox entered dead letter: outboxId={}, templateCode={}, keyVersion={}, status=DEAD, retryCount={}, errorCode={}",
                    task.getId(), task.getTemplateCode(), task.getEncryptionKeyVersion(), retryCount, errorCode, exception);
        } else {
            log.warn("mail outbox retry scheduled: outboxId={}, templateCode={}, keyVersion={}, status=RETRY, retryCount={}, errorCode={}",
                    task.getId(), task.getTemplateCode(), task.getEncryptionKeyVersion(), retryCount, errorCode);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
