package com.hootoom.forum.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 可靠邮件任务实体，收件地址和模板参数在入库前完成信封加密。 */
@Data
public class MailOutbox {
    private Long id;
    private String templateCode;
    private byte[] recipientCiphertext;
    private byte[] payloadCiphertext;
    private String encryptionKeyVersion;
    private String businessKey;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime sentAt;
    private String lastErrorCode;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
