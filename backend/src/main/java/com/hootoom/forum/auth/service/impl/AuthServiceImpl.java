package com.hootoom.forum.auth.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hootoom.forum.auth.dto.ConfirmEmailDTO;
import com.hootoom.forum.auth.dto.RegisterDTO;
import com.hootoom.forum.auth.dto.LoginDTO;
import com.hootoom.forum.auth.dto.RequestPasswordResetDTO;
import com.hootoom.forum.auth.dto.ConfirmPasswordResetDTO;
import com.hootoom.forum.auth.dto.RequestEmailVerificationDTO;
import com.hootoom.forum.auth.entity.EmailToken;
import com.hootoom.forum.auth.entity.ForumUser;
import com.hootoom.forum.auth.entity.MailOutbox;
import com.hootoom.forum.auth.entity.RefreshToken;
import com.hootoom.forum.auth.exception.TokenReplayException;
import com.hootoom.forum.auth.mapper.EmailTokenMapper;
import com.hootoom.forum.auth.mapper.ForumUserMapper;
import com.hootoom.forum.auth.mapper.MailOutboxMapper;
import com.hootoom.forum.auth.mapper.RefreshTokenMapper;
import com.hootoom.forum.auth.service.AuthService;
import com.hootoom.forum.auth.service.LoginRateLimitService;
import com.hootoom.forum.auth.service.PasswordResetRateLimitService;
import com.hootoom.forum.auth.vo.EmailVerificationVO;
import com.hootoom.forum.auth.vo.RegistrationVO;
import com.hootoom.forum.auth.vo.AuthSessionVO;
import com.hootoom.forum.auth.vo.CurrentUserVO;
import com.hootoom.forum.auth.vo.LogoutVO;
import com.hootoom.forum.auth.vo.PasswordResetVO;
import com.hootoom.forum.auth.vo.SessionVO;
import com.hootoom.forum.common.exception.BusinessException;
import com.hootoom.forum.security.EnvelopeEncryptionService;
import com.hootoom.forum.security.SecureTokenService;
import com.hootoom.forum.security.AccessTokenService;
import com.hootoom.forum.governance.service.SensitiveTextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String VERIFY_EMAIL = "VERIFY_EMAIL";
    private static final String RESET_PASSWORD = "RESET_PASSWORD";

    private final ForumUserMapper userMapper;
    private final EmailTokenMapper emailTokenMapper;
    private final MailOutboxMapper mailOutboxMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService tokenService;
    private final AccessTokenService accessTokenService;
    private final LoginRateLimitService loginRateLimitService;
    private final PasswordResetRateLimitService passwordResetRateLimitService;
    private final SensitiveTextService sensitiveTextService;
    private final EnvelopeEncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final int verificationTokenMinutes;
    private final int refreshTokenDays;
    private final int passwordResetTokenMinutes;
    private final String currentPolicyVersion;
    private final String verificationBaseUrl;
    private final String passwordResetBaseUrl;
    private final Clock clock;

    // 明确标记生产构造器；另一个带 Clock 的包级构造器仅用于可重复的时间相关测试。
    @Autowired
    public AuthServiceImpl(
            ForumUserMapper userMapper,
            EmailTokenMapper emailTokenMapper,
            MailOutboxMapper mailOutboxMapper,
            RefreshTokenMapper refreshTokenMapper,
            PasswordEncoder passwordEncoder,
            SecureTokenService tokenService,
            AccessTokenService accessTokenService,
            LoginRateLimitService loginRateLimitService,
            PasswordResetRateLimitService passwordResetRateLimitService,
            SensitiveTextService sensitiveTextService,
            EnvelopeEncryptionService encryptionService,
            ObjectMapper objectMapper,
            @Value("${app.auth.verification-token-minutes:30}") int verificationTokenMinutes,
            @Value("${app.auth.refresh-token-days:7}") int refreshTokenDays,
            @Value("${app.auth.password-reset-token-minutes:15}") int passwordResetTokenMinutes,
            @Value("${app.auth.current-policy-version}") String currentPolicyVersion,
            @Value("${app.mail.verification-base-url}") String verificationBaseUrl,
            @Value("${app.mail.password-reset-base-url}") String passwordResetBaseUrl) {
        this(userMapper, emailTokenMapper, mailOutboxMapper, refreshTokenMapper, passwordEncoder, tokenService,
                accessTokenService, loginRateLimitService, passwordResetRateLimitService, sensitiveTextService,
                encryptionService, objectMapper,
                verificationTokenMinutes, refreshTokenDays, passwordResetTokenMinutes, currentPolicyVersion,
                verificationBaseUrl, passwordResetBaseUrl, Clock.systemUTC());
    }

    AuthServiceImpl(ForumUserMapper userMapper, EmailTokenMapper emailTokenMapper,
                    MailOutboxMapper mailOutboxMapper, RefreshTokenMapper refreshTokenMapper,
                    PasswordEncoder passwordEncoder, SecureTokenService tokenService,
                    AccessTokenService accessTokenService, LoginRateLimitService loginRateLimitService,
                    PasswordResetRateLimitService passwordResetRateLimitService,
                    SensitiveTextService sensitiveTextService,
                    EnvelopeEncryptionService encryptionService,
                    ObjectMapper objectMapper, int verificationTokenMinutes, int refreshTokenDays,
                    int passwordResetTokenMinutes, String currentPolicyVersion,
                    String verificationBaseUrl, String passwordResetBaseUrl, Clock clock) {
        this.userMapper = userMapper;
        this.emailTokenMapper = emailTokenMapper;
        this.mailOutboxMapper = mailOutboxMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.accessTokenService = accessTokenService;
        this.loginRateLimitService = loginRateLimitService;
        this.passwordResetRateLimitService = passwordResetRateLimitService;
        this.sensitiveTextService = sensitiveTextService;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.verificationTokenMinutes = verificationTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
        this.passwordResetTokenMinutes = passwordResetTokenMinutes;
        this.currentPolicyVersion = currentPolicyVersion;
        this.verificationBaseUrl = verificationBaseUrl;
        this.passwordResetBaseUrl = passwordResetBaseUrl;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RegistrationVO register(RegisterDTO command) {
        String username = command.username().strip().toLowerCase(Locale.ROOT);
        String email = command.email().strip().toLowerCase(Locale.ROOT);
        validatePolicy(command.policyVersion());
        sensitiveTextService.rejectUnsafeProfileText(username, null);
        ensureUniqueAccount(username, email);

        LocalDateTime now = now();
        ForumUser user = new ForumUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setNickname(username);
        user.setAvatarKey("default-1");
        user.setStatus("PENDING");
        user.setPolicyVersion(currentPolicyVersion);
        user.setPolicyAcceptedAt(now);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // 并发注册仍以数据库唯一索引为最终防线，不把底层索引名暴露给客户端。
            throw new BusinessException("RESOURCE_CONFLICT", "用户名或邮箱已被使用", HttpStatus.CONFLICT);
        }

        createVerificationMail(user, now);
        log.info("user registration accepted: userId={}, status=PENDING", user.getId());
        return new RegistrationVO(String.valueOf(user.getId()), "PENDING", "注册成功，请查收验证邮件");
    }

    @Override
    @Transactional
    public EmailVerificationVO confirmEmail(ConfirmEmailDTO command) {
        LocalDateTime now = now();
        EmailToken token = emailTokenMapper.findByHashForUpdate(tokenService.sha256(command.token()), VERIFY_EMAIL);
        if (token == null || token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new BusinessException("INVALID_REQUEST", "验证链接无效或已过期", HttpStatus.BAD_REQUEST);
        }

        if (userMapper.activatePendingUser(token.getUserId()) != 1 || emailTokenMapper.markUsed(token.getId(), now) != 1) {
            throw new BusinessException("RESOURCE_CONFLICT", "账号状态已发生变化", HttpStatus.CONFLICT);
        }
        log.info("email verification completed: userId={}", token.getUserId());
        return new EmailVerificationVO("ACTIVE", "邮箱验证成功");
    }

    @Override
    @Transactional
    public EmailVerificationVO requestEmailVerification(RequestEmailVerificationDTO command) {
        ForumUser user = userMapper.findByEmail(command.email().strip().toLowerCase(Locale.ROOT));
        if (user != null && "PENDING".equals(user.getStatus())) {
            createVerificationMail(user, now());
            log.info("email verification mail requeued: userId={}", user.getId());
        }
        return new EmailVerificationVO("PENDING", "如果账号需要验证，系统将发送验证邮件");
    }

    @Override
    @Transactional
    public AuthSessionVO login(LoginDTO command, String clientAddress) {
        String identifier = command.identifier().strip().toLowerCase(Locale.ROOT);
        loginRateLimitService.checkAndConsume(clientAddress, identifier);
        ForumUser user = userMapper.findByIdentifier(identifier);
        if (user == null || !passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new BusinessException("UNAUTHENTICATED", "用户名、邮箱或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if ("PENDING".equals(user.getStatus())) {
            throw new BusinessException("EMAIL_NOT_VERIFIED", "请先完成邮箱验证", HttpStatus.FORBIDDEN);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("ACCOUNT_DISABLED", "账号当前不可用", HttpStatus.FORBIDDEN);
        }
        LocalDateTime now = now();
        userMapper.updateLastLoginAt(user.getId(), now);
        loginRateLimitService.resetAccount(identifier);
        log.info("user login succeeded: userId={}", user.getId());
        return createSession(user, UUID.randomUUID().toString(), command.deviceName(), now);
    }

    @Override
    @Transactional(noRollbackFor = TokenReplayException.class)
    public AuthSessionVO refresh(String rawRefreshToken) {
        LocalDateTime now = now();
        RefreshToken current = refreshTokenMapper.findByHashForUpdate(tokenService.sha256(rawRefreshToken));
        if (current == null || !current.getExpiresAt().isAfter(now)) {
            throw new BusinessException("UNAUTHENTICATED", "登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
        if (current.getRevokedAt() != null) {
            // 已轮换令牌再次出现视为重放，立即撤销整个族，阻止攻击者与合法客户端继续竞争。
            refreshTokenMapper.revokeFamily(current.getFamilyId(), now, "REPLAY_DETECTED");
            log.warn("refresh token replay detected: userId={}, securityEvent=TOKEN_REPLAYED", current.getUserId());
            throw new TokenReplayException();
        }
        ForumUser user = userMapper.findById(current.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            refreshTokenMapper.revokeFamily(current.getFamilyId(), now, "SUBJECT_INACTIVE");
            throw new BusinessException("UNAUTHENTICATED", "登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
        refreshTokenMapper.revokeById(current.getId(), now, "ROTATED");
        return createSession(user, current.getFamilyId(), current.getDeviceName(), now);
    }

    @Override
    @Transactional
    public LogoutVO logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return new LogoutVO(true);
        }
        RefreshToken token = refreshTokenMapper.findByHashForUpdate(tokenService.sha256(rawRefreshToken));
        if (token != null && token.getRevokedAt() == null) {
            refreshTokenMapper.revokeById(token.getId(), now(), "LOGOUT");
        }
        return new LogoutVO(true);
    }

    @Override
    @Transactional
    public PasswordResetVO requestPasswordReset(RequestPasswordResetDTO command, String clientAddress) {
        passwordResetRateLimitService.checkAndConsume(clientAddress);
        String email = command.email().strip().toLowerCase(Locale.ROOT);
        ForumUser user = userMapper.findByEmail(email);
        if (user != null && "ACTIVE".equals(user.getStatus())) {
            createPasswordResetMail(user, now());
            log.info("password reset mail queued: userId={}", user.getId());
        }
        // 无论账号是否存在都返回完全相同的响应，避免利用接口枚举注册邮箱。
        return new PasswordResetVO("如果该邮箱已注册，系统将发送密码重置邮件");
    }

    @Override
    @Transactional
    public PasswordResetVO confirmPasswordReset(ConfirmPasswordResetDTO command) {
        LocalDateTime now = now();
        EmailToken token = emailTokenMapper.findByHashForUpdate(
                tokenService.sha256(command.token()), RESET_PASSWORD);
        if (token == null || token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new BusinessException("INVALID_REQUEST", "重置链接无效或已过期", HttpStatus.BAD_REQUEST);
        }
        if (userMapper.updatePassword(token.getUserId(), passwordEncoder.encode(command.newPassword()), now) != 1
                || emailTokenMapper.markUsed(token.getId(), now) != 1) {
            throw new BusinessException("RESOURCE_CONFLICT", "账号状态已发生变化", HttpStatus.CONFLICT);
        }
        refreshTokenMapper.revokeAllForUser(token.getUserId(), now, "PASSWORD_RESET");
        log.info("password reset completed: userId={}", token.getUserId());
        return new PasswordResetVO("密码重置成功，请重新登录");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionVO> listSessions(Long userId) {
        LocalDateTime now = now();
        return refreshTokenMapper.findActiveByUser(userId, now).stream()
                .map(token -> new SessionVO(String.valueOf(token.getId()), token.getDeviceName(),
                        toInstant(token.getCreatedAt()), toInstant(token.getLastUsedAt()),
                        toInstant(token.getExpiresAt())))
                .toList();
    }

    @Override
    @Transactional
    public LogoutVO revokeSession(Long userId, Long sessionId) {
        refreshTokenMapper.revokeOwnedSession(sessionId, userId, now(), "USER_REVOKED");
        return new LogoutVO(true);
    }

    private AuthSessionVO createSession(ForumUser user, String familyId, String deviceName, LocalDateTime now) {
        String rawRefreshToken = tokenService.generate();
        String csrfToken = tokenService.generate();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSubjectType("USER");
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(tokenService.sha256(rawRefreshToken));
        refreshToken.setFamilyId(familyId);
        refreshToken.setDeviceName(deviceName == null || deviceName.isBlank() ? null : deviceName.strip());
        refreshToken.setExpiresAt(now.plusDays(refreshTokenDays));
        refreshTokenMapper.insert(refreshToken);
        CurrentUserVO userVO = new CurrentUserVO(String.valueOf(user.getId()), user.getUsername(),
                user.getNickname(), user.getAvatarKey());
        return new AuthSessionVO(accessTokenService.issue(user), accessTokenService.expiresInSeconds(),
                userVO, rawRefreshToken, csrfToken);
    }

    private void validatePolicy(String submittedVersion) {
        if (!currentPolicyVersion.equals(submittedVersion)) {
            throw new BusinessException("POLICY_VERSION_OUTDATED", "用户协议版本已更新，请重新确认", HttpStatus.CONFLICT);
        }
    }

    private void ensureUniqueAccount(String username, String email) {
        if (userMapper.existsByUsername(username)) {
            throw new BusinessException("USERNAME_EXISTS", "用户名已被使用", HttpStatus.CONFLICT);
        }
        if (userMapper.existsByEmail(email)) {
            throw new BusinessException("EMAIL_EXISTS", "邮箱已被使用", HttpStatus.CONFLICT);
        }
    }

    private void createVerificationMail(ForumUser user, LocalDateTime now) {
        String rawToken = tokenService.generate();
        emailTokenMapper.invalidateActiveTokens(user.getId(), VERIFY_EMAIL, now);

        EmailToken token = new EmailToken();
        token.setUserId(user.getId());
        token.setPurpose(VERIFY_EMAIL);
        token.setTokenHash(tokenService.sha256(rawToken));
        token.setExpiresAt(now.plusMinutes(verificationTokenMinutes));
        emailTokenMapper.insert(token);

        String verificationUrl = UriComponentsBuilder.fromUriString(verificationBaseUrl)
                .queryParam("token", rawToken)
                .build().encode().toUriString();
        String payload = toJson(Map.of("verificationUrl", verificationUrl,
                "expiresInMinutes", verificationTokenMinutes));

        MailOutbox mail = new MailOutbox();
        mail.setTemplateCode("VERIFY_EMAIL");
        mail.setRecipientCiphertext(encryptionService.encrypt(user.getEmail()));
        mail.setPayloadCiphertext(encryptionService.encrypt(payload));
        mail.setEncryptionKeyVersion(EnvelopeEncryptionService.KEY_VERSION);
        mail.setBusinessKey("verify-email:" + user.getId() + ":" + token.getId());
        mail.setStatus("PENDING");
        mail.setRetryCount(0);
        mail.setNextRetryAt(now);
        mailOutboxMapper.insert(mail);
    }

    private void createPasswordResetMail(ForumUser user, LocalDateTime now) {
        String rawToken = tokenService.generate();
        emailTokenMapper.invalidateActiveTokens(user.getId(), RESET_PASSWORD, now);
        EmailToken token = new EmailToken();
        token.setUserId(user.getId());
        token.setPurpose(RESET_PASSWORD);
        token.setTokenHash(tokenService.sha256(rawToken));
        token.setExpiresAt(now.plusMinutes(passwordResetTokenMinutes));
        emailTokenMapper.insert(token);

        String resetUrl = UriComponentsBuilder.fromUriString(passwordResetBaseUrl)
                .queryParam("token", rawToken).build().encode().toUriString();
        String payload = toJson(Map.of("resetUrl", resetUrl,
                "expiresInMinutes", passwordResetTokenMinutes));
        MailOutbox mail = new MailOutbox();
        mail.setTemplateCode(RESET_PASSWORD);
        mail.setRecipientCiphertext(encryptionService.encrypt(user.getEmail()));
        mail.setPayloadCiphertext(encryptionService.encrypt(payload));
        mail.setEncryptionKeyVersion(EnvelopeEncryptionService.KEY_VERSION);
        mail.setBusinessKey("reset-password:" + user.getId() + ":" + token.getId());
        mail.setStatus("PENDING");
        mail.setRetryCount(0);
        mail.setNextRetryAt(now);
        mailOutboxMapper.insert(mail);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize verification mail payload", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private java.time.Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
