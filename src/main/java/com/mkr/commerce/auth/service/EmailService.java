package com.mkr.commerce.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.base-url}")
    private String baseUrl;

    // ── Password Reset ────────────────────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String toEmail, String toName, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        boolean sent = sendHtmlEmail(toEmail,
                "Reset your MKR Commerce password",
                buildPasswordResetHtml(toName, resetLink));
        if (sent) {
            log.info("Password reset email sent to: {}", toEmail);
        } else {
            log.warn("Password reset email failed for: {} — link: {}", toEmail, resetLink);
        }
    }

    // ── Staff Invitation ──────────────────────────────────────────────────

    @Async
    public void sendInvitationEmail(String toEmail, String toName,
                                    String invitedByName, String invitationToken) {
        String setPasswordLink = baseUrl + "/set-password?token=" + invitationToken;
        boolean sent = sendHtmlEmail(toEmail,
                "You've been invited to MKR Commerce Admin",
                buildInvitationHtml(toName, invitedByName, toEmail, setPasswordLink));
        if (sent) {
            log.info("Invitation email sent to: {}", toEmail);
        } else {
            log.warn("Invitation email failed for: {} — link: {}", toEmail, setPasswordLink);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, "MKR Commerce");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email to {} — {}: {}", to,
                    ex.getClass().getSimpleName(), ex.getMessage());
            return false;
        }
    }

    private String buildInvitationHtml(String name, String invitedBy,
                                       String email, String setPasswordLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #f3f4f6; margin: 0; padding: 0; }
                .wrapper { max-width: 560px; margin: 40px auto; background: #fff; border-radius: 12px;
                           box-shadow: 0 4px 24px rgba(0,0,0,.08); overflow: hidden; }
                .header { background: linear-gradient(135deg, #1e1b4b, #2874F0); padding: 32px 40px; }
                .header h1 { color: #fff; margin: 0; font-size: 22px; font-weight: 700; }
                .header p  { color: rgba(255,255,255,.70); margin: 4px 0 0; font-size: 13px; }
                .body { padding: 36px 40px; }
                .body p { color: #374151; font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
                .account-box { background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 8px;
                               padding: 12px 16px; margin-bottom: 20px; font-size: 14px; color: #0369a1; }
                .account-box strong { display: block; font-size: 15px; margin-bottom: 2px; color: #0c4a6e; }
                .btn-wrap { text-align: center; margin: 24px 0; }
                .btn { display: inline-block; padding: 15px 36px; background: #2874F0;
                       color: #fff !important; text-decoration: none !important; border-radius: 10px;
                       font-weight: 700; font-size: 16px; letter-spacing: .2px; }
                .note { font-size: 13px !important; color: #6B7280 !important; }
                .divider { border: none; border-top: 1px solid #e5e7eb; margin: 24px 0; }
                .fallback { font-size: 12px; color: #9ca3af; text-align: center; margin-top: 16px; }
                .fallback a { color: #2874F0; word-break: break-all; }
                .footer { background: #f9fafb; border-top: 1px solid #e5e7eb;
                          padding: 18px 40px; font-size: 12px; color: #9ca3af; text-align: center; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header">
                  <h1>MKR Commerce</h1>
                  <p>You've been invited to join the Admin Portal</p>
                </div>
                <div class="body">
                  <p>Hi <strong>%s</strong>,</p>
                  <p><strong>%s</strong> has created a staff account for you on the
                    <strong>MKR Commerce Admin</strong> portal.</p>
                  <div class="account-box">
                    <strong>%s</strong>
                    Use this email address to log in after setting your password.
                  </div>
                  <p>Click the button below to set your password and activate your account.
                    This link is valid for <strong>48 hours</strong>.</p>
                  <div class="btn-wrap">
                    <a href="%s" class="btn">Set Up My Account &rarr;</a>
                  </div>
                  <hr class="divider"/>
                  <p class="note">If you weren't expecting this, you can safely ignore it.</p>
                  <div class="fallback">
                    Button not working? <a href="%s">Click here</a>
                  </div>
                </div>
                <div class="footer">
                  &copy; MKR Commerce &nbsp;&bull;&nbsp; Automated message — do not reply.
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, invitedBy, email, setPasswordLink, setPasswordLink);
    }

    private String buildPasswordResetHtml(String name, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #f3f4f6; margin: 0; padding: 0; }
                .wrapper { max-width: 560px; margin: 40px auto; background: #fff; border-radius: 12px;
                           box-shadow: 0 4px 24px rgba(0,0,0,.08); overflow: hidden; }
                .header { background: linear-gradient(135deg, #1e1b4b, #2874F0); padding: 32px 40px; }
                .header h1 { color: #fff; margin: 0; font-size: 22px; font-weight: 700; }
                .header p  { color: rgba(255,255,255,.70); margin: 4px 0 0; font-size: 13px; }
                .body { padding: 36px 40px; }
                .body p { color: #374151; font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
                .btn-wrap { text-align: center; margin: 24px 0; }
                .btn { display: inline-block; padding: 15px 36px; background: #2874F0;
                       color: #fff !important; text-decoration: none !important; border-radius: 10px;
                       font-weight: 700; font-size: 16px; }
                .note { font-size: 13px !important; color: #6B7280 !important; }
                .divider { border: none; border-top: 1px solid #e5e7eb; margin: 24px 0; }
                .fallback { font-size: 12px; color: #9ca3af; text-align: center; margin-top: 16px; }
                .fallback a { color: #2874F0; word-break: break-all; }
                .footer { background: #f9fafb; border-top: 1px solid #e5e7eb;
                          padding: 18px 40px; font-size: 12px; color: #9ca3af; text-align: center; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header">
                  <h1>MKR Commerce</h1>
                  <p>Password Reset Request</p>
                </div>
                <div class="body">
                  <p>Hi <strong>%s</strong>,</p>
                  <p>We received a request to reset your password. Click the button below.</p>
                  <div class="btn-wrap">
                    <a href="%s" class="btn">Reset My Password &rarr;</a>
                  </div>
                  <hr class="divider"/>
                  <p class="note">
                    This link expires in <strong>60 minutes</strong>.<br/>
                    If you did not request this, ignore this email — your password won't change.
                  </p>
                  <div class="fallback">
                    Button not working? <a href="%s">Click here</a>
                  </div>
                </div>
                <div class="footer">
                  &copy; MKR Commerce &nbsp;&bull;&nbsp; Automated message — do not reply.
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, resetLink, resetLink);
    }
}
