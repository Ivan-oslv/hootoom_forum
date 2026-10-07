package com.hootoom.forum.mail.job;

import com.hootoom.forum.auth.entity.MailOutbox;
import com.hootoom.forum.mail.service.MailOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.UUID;

@Component
public class MailOutboxJob {
    private static final Logger log = LoggerFactory.getLogger(MailOutboxJob.class);
    private final MailOutboxService service;
    private final String workerId;

    public MailOutboxJob(MailOutboxService service, @Value("${spring.application.name}") String applicationName) {
        this.service = service;
        this.workerId = applicationName + "-" + ManagementFactory.getRuntimeMXBean().getName();
    }

    @Scheduled(fixedDelayString = "${app.mail.outbox-poll-delay-ms:5000}")
    public void dispatch() {
        String executionId = UUID.randomUUID().toString();
        MDC.put("requestId", executionId);
        long started = System.nanoTime();
        try {
            List<MailOutbox> tasks = service.claimBatch(workerId);
            for (MailOutbox task : tasks) {
                service.deliver(task, workerId);
            }
            if (!tasks.isEmpty()) {
                log.info("mail outbox job completed: jobName=mailOutbox, executionId={}, processed={}, durationMs={}",
                        executionId, tasks.size(), (System.nanoTime() - started) / 1_000_000);
            }
        } finally {
            MDC.remove("requestId");
        }
    }
}
