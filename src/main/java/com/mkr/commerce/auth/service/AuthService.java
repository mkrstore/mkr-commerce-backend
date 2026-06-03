package com.mkr.commerce.auth.service;

import com.mkr.commerce.auth.dto.*;
import com.mkr.commerce.auth.entity.PasswordHistory;
import com.mkr.commerce.auth.entity.PasswordResetToken;
import com.mkr.commerce.auth.entity.RefreshToken;
import com.mkr.commerce.auth.repository.PasswordHistoryRepository;
import com.mkr.commerce.auth.repository.PasswordResetTokenRepository;
import com.mkr.commerce.auth.repository.RefreshTokenRepository;
import com.mkr.commerce.auth.security.JwtService;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ErrorCode;
import com.mkr.commerce.common.exception.UnauthorizedException;
import com.mkr.commerce.user.entity.InvitationToken;
import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.AuditAction;
import com.mkr.commerce.user.repository.InvitationTokenRepository;
import com.mkr.commerce.user.repository.UserRepository;
import com.mkr.commerce.user.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository               userRepository;
    private final RefreshTokenRepository       refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordHistoryRepository    passwordHistoryRepository;
    private final InvitationTokenRepository    invitationTokenRepository;
    private final JwtService                   jwtService;
    private final PasswordEncoder              passwordEncoder;
    private final EmailService                 emailService;
    private final AuditLogService              auditLogService;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Value("${app.password-reset.expiry-minutes}")
    private int resetTokenExpiryMinutes;

    @Value("${app.password-reset.history-count}")
    private int passwordHistoryCount;

    // ── Login ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthTokenPair login(LoginRequest request) {
        // Resolve identifier — try email first, then mobile number
        String raw = request.identifier().trim();
        User user = resolveByIdentifier(raw)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials.", ErrorCode.INVALID_CREDENTIALS));

        if (!user.isActive()) {
            throw new UnauthorizedException("Your account has been deactivated. Contact your administrator.", ErrorCode.ACCOUNT_DEACTIVATED);
        }

        if (user.getPasswordHash() == null) {
            // Distinguish: Google-only account vs pending invitation (never set password)
            boolean hasPendingInvitation = invitationTokenRepository
                    .findActiveByUser(user, Instant.now()).isPresent();
            if (hasPendingInvitation) {
                throw new UnauthorizedException(
                        "Your account is not activated yet. Please check your invitation email or use 'Continue with Google' to set your password.",
                        ErrorCode.INVITATION_TOKEN_INVALID);
            }
            throw new UnauthorizedException("This account uses Google Sign-In. Please use 'Continue with Google'.", ErrorCode.GOOGLE_LOGIN_REQUIRED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials.", ErrorCode.INVALID_CREDENTIALS);
        }

        auditLogService.log(user.getId(), user, AuditAction.LOGIN, null);
        log.info("Login: {} [{}]", user.getEmail(), user.getRole());
        return issueTokenPair(user);
    }

    private java.util.Optional<User> resolveByIdentifier(String identifier) {
        // Looks like an email
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier.toLowerCase());
        }
        // Strip common phone prefixes and look up by mobile number
        String phone = identifier.replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("+91")) phone = phone.substring(3);
        else if (phone.startsWith("91") && phone.length() == 12) phone = phone.substring(2);
        return userRepository.findByMobileNumber(phone);
    }

    // ── Refresh ────────────────────────────────────────────────────────────

    @Transactional
    public AuthTokenPair refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("No session cookie found. Please log in.", ErrorCode.NO_SESSION);
        }

        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session. Please log in again.", ErrorCode.REFRESH_TOKEN_INVALID));

        if (!stored.isValid()) {
            // Possible token reuse attack — revoke all sessions for this user
            log.warn("Refresh token reuse detected for user: {}", stored.getUser().getEmail());
            refreshTokenRepository.revokeAllByUser(stored.getUser());
            throw new UnauthorizedException("Session expired. Please log in again.", ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokenPair(stored.getUser());
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshTokenRepository.findByToken(rawRefreshToken)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("Logout: {}", rt.getUser().getEmail());
                });
    }

    // ── Me ──────────────────────────────────────────────────────────────────

    public AuthUserDto getMe(User user) {
        return AuthUserDto.from(user);
    }

    // ── Forgot Password ────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Find user — always return a generic success message even if email
        //    doesn't exist to prevent user enumeration attacks
        userRepository.findByEmail(request.email().toLowerCase().trim())
                .ifPresent(user -> {
                    if (!user.isActive()) return;

                    // 3. Invalidate any existing active reset tokens for this user
                    resetTokenRepository.invalidateAllForUser(user);

                    // 4. Generate new reset token
                    String rawToken = UUID.randomUUID().toString();
                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            .user(user)
                            .token(rawToken)
                            .expiresAt(Instant.now().plus(resetTokenExpiryMinutes, ChronoUnit.MINUTES))
                            .build();
                    resetTokenRepository.save(resetToken);

                    // 5. Send email (async — won't block response)
                    emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);

                    log.info("Password reset token issued for: {}", user.getEmail());
                });
    }

    // ── Validate Reset Token ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ValidateResetTokenResponse validateResetToken(String token) {
        return resetTokenRepository.findByToken(token)
                .map(rt -> {
                    if (!rt.isValid()) {
                        return new ValidateResetTokenResponse(false, null,
                                "This reset link has expired or has already been used.");
                    }
                    return new ValidateResetTokenResponse(true, maskEmail(rt.getUser().getEmail()),
                            "Token is valid.");
                })
                .orElse(new ValidateResetTokenResponse(false, null, "Invalid reset link."));
    }

    // ── Reset Password ─────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Find and validate token
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link. Please request a new one.", ErrorCode.RESET_TOKEN_INVALID));

        if (resetToken.isExpired()) {
            throw new BadRequestException("This reset link has expired. Please request a new one.", ErrorCode.RESET_TOKEN_EXPIRED);
        }
        if (resetToken.isUsed()) {
            throw new BadRequestException("This reset link has already been used. Please request a new one.", ErrorCode.RESET_TOKEN_INVALID);
        }

        User user = resetToken.getUser();

        // 3. Check password history — reject if matches last N passwords
        List<PasswordHistory> history = passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user);
        for (PasswordHistory past : history) {
            if (passwordEncoder.matches(request.newPassword(), past.getPasswordHash())) {
                throw new BadRequestException(
                        "You cannot reuse your last " + passwordHistoryCount + " passwords. Please choose a different password.",
                        ErrorCode.PASSWORD_RECENTLY_USED
                );
            }
        }

        // 4. Also check current password
        if (user.getPasswordHash() != null && passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as your current password.", ErrorCode.PASSWORD_RECENTLY_USED);
        }

        // 5. Save current password to history before replacing
        if (user.getPasswordHash() != null) {
            passwordHistoryRepository.save(PasswordHistory.builder()
                    .user(user)
                    .passwordHash(user.getPasswordHash())
                    .build());
        }

        // 6. Update password
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // 7. Mark reset token as used
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        // 8. Prune old history beyond allowed count
        passwordHistoryRepository.pruneOldEntries(user.getId(), passwordHistoryCount);

        // 9. Revoke all active sessions — force re-login with new password
        refreshTokenRepository.revokeAllByUser(user);

        auditLogService.log(user.getId(), null, AuditAction.PASSWORD_RESET, null);
        log.info("Password reset successful for: {}", user.getEmail());
    }

    // ── First Password (dev env — no invitation token required) ───────────────

    @Transactional
    public AuthTokenPair setFirstPassword(FirstPasswordRequest request) {
        String raw = request.identifier().trim();
        User user = resolveByIdentifier(raw)
                .orElseThrow(() -> new BadRequestException("No account found for this identifier.", ErrorCode.RESOURCE_NOT_FOUND));

        if (user.getPasswordHash() != null) {
            throw new BadRequestException("This account already has a password. Use the login form or 'Forgot Password'.", ErrorCode.INVITATION_TOKEN_INVALID);
        }

        boolean hasPendingInvitation = invitationTokenRepository
                .findActiveByUser(user, Instant.now()).isPresent();
        if (!hasPendingInvitation) {
            throw new BadRequestException("No pending invitation found for this account.", ErrorCode.INVITATION_TOKEN_INVALID);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setActive(true);
        userRepository.save(user);

        invitationTokenRepository.invalidateAllForUser(user);
        auditLogService.log(user.getId(), null, AuditAction.INVITATION_ACCEPTED, "via first-password dev flow");
        log.info("First password set for: {}", user.getEmail());
        return issueTokenPair(user);
    }

    // ── Direct Password Reset (dev env — no token required) ──────────────

    @Transactional
    public AuthTokenPair directResetPassword(DirectResetRequest request) {
        String raw = request.identifier().trim();
        User user = resolveByIdentifier(raw)
                .orElseThrow(() -> new BadRequestException("No account found for this identifier.", ErrorCode.RESOURCE_NOT_FOUND));
        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated. Contact your admin.", ErrorCode.ACCOUNT_DEACTIVATED);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditLogService.log(user.getId(), null, AuditAction.PASSWORD_RESET, "via dev direct-reset");
        log.info("Dev direct password reset for: {}", user.getEmail());
        return issueTokenPair(user);
    }

    // ── Google OAuth2 ──────────────────────────────────────────────────────

    @Transactional
    public AuthTokenPair loginWithGoogle(User user) {
        auditLogService.log(user.getId(), user, AuditAction.LOGIN, "via Google");
        log.info("Google login: {} [{}]", user.getEmail(), user.getRole());
        return issueTokenPair(user);
    }

    // ── Validate Invitation Token ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public ValidateResetTokenResponse validateInvitation(String token) {
        return invitationTokenRepository.findByToken(token)
                .map(it -> {
                    if (!it.isValid()) {
                        return new ValidateResetTokenResponse(false, null,
                                "This invitation link has expired or has already been used.");
                    }
                    return new ValidateResetTokenResponse(true, maskEmail(it.getUser().getEmail()),
                            "Invitation is valid.");
                })
                .orElse(new ValidateResetTokenResponse(false, null, "Invalid invitation link."));
    }

    // ── Set Password (new staff account activation) ────────────────────────

    @Transactional
    public void setPassword(SetPasswordRequest request) {
        InvitationToken invitation = invitationTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired invitation link. Please ask your admin to resend.", ErrorCode.INVITATION_TOKEN_INVALID));

        if (invitation.isExpired()) {
            throw new BadRequestException(
                    "This invitation link has expired. Please ask your admin to resend.", ErrorCode.INVITATION_TOKEN_EXPIRED);
        }
        if (invitation.isUsed()) {
            throw new BadRequestException(
                    "This invitation has already been used. If you need help, contact your admin.", ErrorCode.INVITATION_TOKEN_INVALID);
        }

        User user = invitation.getUser();

        // Set password and activate account
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setActive(true);
        userRepository.save(user);

        // Mark token as used
        invitation.setUsed(true);
        invitationTokenRepository.save(invitation);

        auditLogService.log(user.getId(), null, AuditAction.INVITATION_ACCEPTED, null);
        log.info("Account activated via invitation: {}", user.getEmail());
    }

    // ── Private ─────────────────────────────────────────────────────────────

    /**
     * Creates an access token + persists a refresh token.
     * Single point for login and token rotation.
     */
    private AuthTokenPair issueTokenPair(User user) {
        String accessToken     = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshTokenValue();

        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(rawRefreshToken)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiryMs))
                .build();
        refreshTokenRepository.save(rt);

        return new AuthTokenPair(accessToken, rawRefreshToken, AuthUserDto.from(user));
    }

    /**
     * Masks email for display: rajesh.kumar@mkr.com → r*****r@mkr.com
     * Reassures the user it's their account without exposing the full email.
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        String local  = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String masked = local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1);
        return masked + domain;
    }
}
