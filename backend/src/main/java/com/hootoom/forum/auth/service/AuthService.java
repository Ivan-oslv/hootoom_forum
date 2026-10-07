package com.hootoom.forum.auth.service;

import com.hootoom.forum.auth.dto.ConfirmEmailDTO;
import com.hootoom.forum.auth.dto.RegisterDTO;
import com.hootoom.forum.auth.dto.LoginDTO;
import com.hootoom.forum.auth.dto.RequestPasswordResetDTO;
import com.hootoom.forum.auth.dto.ConfirmPasswordResetDTO;
import com.hootoom.forum.auth.dto.RequestEmailVerificationDTO;
import com.hootoom.forum.auth.vo.AuthSessionVO;
import com.hootoom.forum.auth.vo.EmailVerificationVO;
import com.hootoom.forum.auth.vo.RegistrationVO;
import com.hootoom.forum.auth.vo.LogoutVO;
import com.hootoom.forum.auth.vo.PasswordResetVO;
import com.hootoom.forum.auth.vo.SessionVO;

import java.util.List;

public interface AuthService {
    RegistrationVO register(RegisterDTO command);
    EmailVerificationVO confirmEmail(ConfirmEmailDTO command);
    EmailVerificationVO requestEmailVerification(RequestEmailVerificationDTO command);
    AuthSessionVO login(LoginDTO command, String clientAddress);
    AuthSessionVO refresh(String rawRefreshToken);
    LogoutVO logout(String rawRefreshToken);
    PasswordResetVO requestPasswordReset(RequestPasswordResetDTO command, String clientAddress);
    PasswordResetVO confirmPasswordReset(ConfirmPasswordResetDTO command);
    List<SessionVO> listSessions(Long userId);
    LogoutVO revokeSession(Long userId, Long sessionId);
}
