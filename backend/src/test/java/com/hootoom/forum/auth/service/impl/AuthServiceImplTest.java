package com.hootoom.forum.auth.service.impl;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hootoom.forum.auth.dto.ConfirmEmailDTO;
import com.hootoom.forum.auth.dto.RegisterDTO;
import com.hootoom.forum.auth.entity.EmailToken;
import com.hootoom.forum.auth.entity.ForumUser;
import com.hootoom.forum.auth.entity.MailOutbox;
import com.hootoom.forum.auth.entity.RefreshToken;
import com.hootoom.forum.auth.mapper.EmailTokenMapper;
import com.hootoom.forum.auth.mapper.ForumUserMapper;
import com.hootoom.forum.auth.mapper.MailOutboxMapper;
import com.hootoom.forum.auth.mapper.RefreshTokenMapper;
import com.hootoom.forum.auth.service.LoginRateLimitService;
import com.hootoom.forum.auth.service.PasswordResetRateLimitService;
import com.hootoom.forum.governance.service.SensitiveTextService;
import com.hootoom.forum.common.exception.BusinessException;
import com.hootoom.forum.security.EnvelopeEncryptionService;
import com.hootoom.forum.security.SecureTokenService;
import com.hootoom.forum.security.AccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private ForumUserMapper userMapper;
    @Mock private EmailTokenMapper tokenMapper;
    @Mock private MailOutboxMapper outboxMapper;
    @Mock private RefreshTokenMapper refreshTokenMapper;
    @Mock private AccessTokenService accessTokenService;
    @Mock private LoginRateLimitService loginRateLimitService;
    @Mock private PasswordResetRateLimitService passwordResetRateLimitService;
    @Mock private SensitiveTextService sensitiveTextService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        String key = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef"
                .getBytes(StandardCharsets.UTF_8));
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T08:00:00Z"), ZoneOffset.UTC);
        service = new AuthServiceImpl(userMapper, tokenMapper, outboxMapper, refreshTokenMapper,
                new BCryptPasswordEncoder(4), new SecureTokenService(), accessTokenService,
                loginRateLimitService, passwordResetRateLimitService, sensitiveTextService,
                new EnvelopeEncryptionService(key),
                JsonMapper.builder().build(), 30, 7, 15, "2026-10-07",
                "http://localhost:3000/verify-email", "http://localhost:3000/reset-password", clock);
    }

    @Test
    void registerNormalizesIdentityAndPersistsOnlyTokenHashAndEncryptedMail() {
        doAnswer(invocation -> {
            invocation.<ForumUser>getArgument(0).setId(10L);
            return 1;
        }).when(userMapper).insert(any(ForumUser.class));
        doAnswer(invocation -> {
            invocation.<EmailToken>getArgument(0).setId(20L);
            return 1;
        }).when(tokenMapper).insert(any(EmailToken.class));

        var result = service.register(new RegisterDTO(
                " Test_User ", " User@Example.COM ", "password123", "2026-10-07", true));

        ArgumentCaptor<ForumUser> userCaptor = ArgumentCaptor.forClass(ForumUser.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("test_user");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).doesNotContain("password123");

        ArgumentCaptor<EmailToken> tokenCaptor = ArgumentCaptor.forClass(EmailToken.class);
        verify(tokenMapper).insert(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);

        ArgumentCaptor<MailOutbox> mailCaptor = ArgumentCaptor.forClass(MailOutbox.class);
        verify(outboxMapper).insert(mailCaptor.capture());
        assertThat(new String(mailCaptor.getValue().getRecipientCiphertext(), StandardCharsets.UTF_8))
                .doesNotContain("user@example.com");
        assertThat(result.userId()).isEqualTo("10");
    }

    @Test
    void registerRejectsOutdatedPolicyVersionBeforeWritingData() {
        assertThatThrownBy(() -> service.register(new RegisterDTO(
                "test_user", "user@example.com", "password123", "old", true)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("POLICY_VERSION_OUTDATED");
    }

    @Test
    void confirmEmailActivatesPendingUserAndConsumesToken() {
        EmailToken token = new EmailToken();
        token.setId(20L);
        token.setUserId(10L);
        token.setExpiresAt(java.time.LocalDateTime.parse("2026-10-07T08:30:00"));
        when(tokenMapper.findByHashForUpdate(any(), eq("VERIFY_EMAIL"))).thenReturn(token);
        when(userMapper.activatePendingUser(10L)).thenReturn(1);
        when(tokenMapper.markUsed(eq(20L), any())).thenReturn(1);

        var result = service.confirmEmail(new ConfirmEmailDTO("valid-token"));

        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(userMapper).activatePendingUser(10L);
        verify(tokenMapper).markUsed(eq(20L), any());
    }

    @Test
    void loginCreatesHashedRefreshSessionForActiveUser() {
        ForumUser user = activeUser();
        when(userMapper.findByIdentifier("demo_user")).thenReturn(user);
        when(accessTokenService.issue(user)).thenReturn("signed-access-token");
        when(accessTokenService.expiresInSeconds()).thenReturn(900L);

        var result = service.login(new com.hootoom.forum.auth.dto.LoginDTO(
                "DEMO_USER", "password123", "Chrome"), "127.0.0.1");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(result.refreshToken()).hasSize(64);
        assertThat(result.accessToken()).isEqualTo("signed-access-token");
        verify(loginRateLimitService).resetAccount("demo_user");
    }

    @Test
    void refreshTokenReplayRevokesWholeFamily() {
        RefreshToken token = new RefreshToken();
        token.setId(30L);
        token.setUserId(10L);
        token.setFamilyId("family-1");
        token.setRevokedAt(java.time.LocalDateTime.parse("2026-10-07T07:00:00"));
        token.setExpiresAt(java.time.LocalDateTime.parse("2026-10-08T08:00:00"));
        when(refreshTokenMapper.findByHashForUpdate(any())).thenReturn(token);

        assertThatThrownBy(() -> service.refresh("replayed-token"))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("TOKEN_REPLAYED");
        verify(refreshTokenMapper).revokeFamily(eq("family-1"), any(), eq("REPLAY_DETECTED"));
        verify(refreshTokenMapper, never()).insert(any());
    }

    @Test
    void passwordResetRequestDoesNotRevealUnknownEmail() {
        when(userMapper.findByEmail("unknown@example.com")).thenReturn(null);

        var result = service.requestPasswordReset(
                new com.hootoom.forum.auth.dto.RequestPasswordResetDTO("UNKNOWN@example.com"), "127.0.0.1");

        assertThat(result.message()).contains("如果该邮箱已注册");
        verify(tokenMapper, never()).insert(any());
        verify(outboxMapper, never()).insert(any());
    }

    @Test
    void passwordResetConsumesTokenIncrementsVersionAndRevokesAllSessions() {
        EmailToken token = new EmailToken();
        token.setId(50L);
        token.setUserId(10L);
        token.setExpiresAt(java.time.LocalDateTime.parse("2026-10-07T08:15:00"));
        when(tokenMapper.findByHashForUpdate(any(), eq("RESET_PASSWORD"))).thenReturn(token);
        when(userMapper.updatePassword(eq(10L), any(), any())).thenReturn(1);
        when(tokenMapper.markUsed(eq(50L), any())).thenReturn(1);

        service.confirmPasswordReset(new com.hootoom.forum.auth.dto.ConfirmPasswordResetDTO(
                "reset-token", "newPassword123"));

        verify(userMapper).updatePassword(eq(10L), any(), any());
        verify(refreshTokenMapper).revokeAllForUser(eq(10L), any(), eq("PASSWORD_RESET"));
    }

    private ForumUser activeUser() {
        ForumUser user = new ForumUser();
        user.setId(10L);
        user.setUsername("demo_user");
        user.setNickname("Demo");
        user.setAvatarKey("default-1");
        user.setStatus("ACTIVE");
        user.setTokenVersion(0);
        user.setPasswordHash(new BCryptPasswordEncoder(4).encode("password123"));
        return user;
    }
}
