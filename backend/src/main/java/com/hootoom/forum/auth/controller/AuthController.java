package com.hootoom.forum.auth.controller;

import com.hootoom.forum.auth.dto.ConfirmEmailDTO;
import com.hootoom.forum.auth.dto.RegisterDTO;
import com.hootoom.forum.auth.dto.LoginDTO;
import com.hootoom.forum.auth.dto.RequestPasswordResetDTO;
import com.hootoom.forum.auth.dto.ConfirmPasswordResetDTO;
import com.hootoom.forum.auth.dto.RequestEmailVerificationDTO;
import com.hootoom.forum.auth.service.AuthService;
import com.hootoom.forum.auth.service.RegistrationRateLimitService;
import com.hootoom.forum.auth.service.TokenConfirmationRateLimitService;
import com.hootoom.forum.auth.vo.EmailVerificationVO;
import com.hootoom.forum.auth.vo.RegistrationVO;
import com.hootoom.forum.auth.vo.AuthSessionVO;
import com.hootoom.forum.auth.vo.LogoutVO;
import com.hootoom.forum.auth.vo.PasswordResetVO;
import com.hootoom.forum.auth.vo.SessionVO;
import com.hootoom.forum.security.RefreshRequestGuard;
import com.hootoom.forum.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.constraints.Positive;

import java.util.List;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RegistrationRateLimitService registrationRateLimitService;
    private final RefreshRequestGuard refreshRequestGuard;
    private final TokenConfirmationRateLimitService tokenConfirmationRateLimitService;
    private final boolean cookieSecure;
    private final long refreshMaxAge;

    public AuthController(AuthService authService, RegistrationRateLimitService registrationRateLimitService,
                          RefreshRequestGuard refreshRequestGuard,
                          TokenConfirmationRateLimitService tokenConfirmationRateLimitService,
                          @Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
                          @Value("${app.auth.refresh-token-days:7}") long refreshDays) {
        this.authService = authService;
        this.registrationRateLimitService = registrationRateLimitService;
        this.refreshRequestGuard = refreshRequestGuard;
        this.tokenConfirmationRateLimitService = tokenConfirmationRateLimitService;
        this.cookieSecure = cookieSecure;
        this.refreshMaxAge = java.time.Duration.ofDays(refreshDays).toSeconds();
    }



    
    // 注册
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationVO>> register(
            @Valid @RequestBody RegisterDTO command, HttpServletRequest request) {
        // 只使用容器确认的直连地址；未经受信代理配置时不接受客户端伪造的转发头。
        registrationRateLimitService.checkAndConsume(request.getRemoteAddr());
        return ResponseEntity.status(201)
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(authService.register(command)));
    }

    // 邮箱验证
    @PostMapping("/email-verifications")
    public ResponseEntity<ApiResponse<EmailVerificationVO>> requestEmailVerification(
            @Valid @RequestBody RequestEmailVerificationDTO command, HttpServletRequest request) {
        registrationRateLimitService.checkAndConsume(request.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(authService.requestEmailVerification(command)));
    }

    @PostMapping("/email-verifications/confirm")
    public ResponseEntity<ApiResponse<EmailVerificationVO>> confirmEmail(
            @Valid @RequestBody ConfirmEmailDTO command, HttpServletRequest request) {
        tokenConfirmationRateLimitService.checkAndConsume(request.getRemoteAddr());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(authService.confirmEmail(command)));
    }
    // 登录

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthSessionVO>> login(@Valid @RequestBody LoginDTO command,
                                                             HttpServletRequest request) {
        return sessionResponse(authService.login(command, request.getRemoteAddr()));
    }



    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthSessionVO>> refresh(HttpServletRequest request) {
        refreshRequestGuard.verify(request);
        String refreshToken = refreshRequestGuard.refreshToken(request);
        if (refreshToken == null) {
            throw new com.hootoom.forum.common.exception.BusinessException(
                    "UNAUTHENTICATED", "登录状态已失效", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return sessionResponse(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutVO>> logout(HttpServletRequest request) {
        refreshRequestGuard.verify(request);
        LogoutVO result = authService.logout(refreshRequestGuard.refreshToken(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Set-Cookie", clearCookie(RefreshRequestGuard.REFRESH_COOKIE, true, "/api/v1/auth").toString())
                .header("Set-Cookie", clearCookie(RefreshRequestGuard.CSRF_COOKIE, false, "/").toString())
                .body(ApiResponse.success(result));
    }

    @PostMapping("/password-resets")
    public ResponseEntity<ApiResponse<PasswordResetVO>> requestPasswordReset(
            @Valid @RequestBody RequestPasswordResetDTO command, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(authService.requestPasswordReset(command, request.getRemoteAddr())));
    }

    @PostMapping("/password-resets/confirm")
    public ResponseEntity<ApiResponse<PasswordResetVO>> confirmPasswordReset(
            @Valid @RequestBody ConfirmPasswordResetDTO command, HttpServletRequest request) {
        tokenConfirmationRateLimitService.checkAndConsume(request.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(authService.confirmPasswordReset(command)));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionVO>>> sessions(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(authService.listSessions(Long.valueOf(jwt.getSubject()))));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<LogoutVO>> revokeSession(
            @AuthenticationPrincipal Jwt jwt, @PathVariable @Positive Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(authService.revokeSession(Long.valueOf(jwt.getSubject()), id)));
    }

    private ResponseEntity<ApiResponse<AuthSessionVO>> sessionResponse(AuthSessionVO session) {
        ResponseCookie refreshCookie = ResponseCookie.from(RefreshRequestGuard.REFRESH_COOKIE, session.refreshToken())
                .httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/api/v1/auth")
                .maxAge(refreshMaxAge).build();
        ResponseCookie csrfCookie = ResponseCookie.from(RefreshRequestGuard.CSRF_COOKIE, session.csrfToken())
                .httpOnly(false).secure(cookieSecure).sameSite("Lax").path("/")
                .maxAge(refreshMaxAge).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Set-Cookie", refreshCookie.toString()).header("Set-Cookie", csrfCookie.toString())
                .body(ApiResponse.success(session));
    }

    private ResponseCookie clearCookie(String name, boolean httpOnly, String path) {
        return ResponseCookie.from(name, "").httpOnly(httpOnly).secure(cookieSecure)
                .sameSite("Lax").path(path).maxAge(0).build();
    }
}
