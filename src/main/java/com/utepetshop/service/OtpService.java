package com.utepetshop.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OtpService {
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private final EmailService emailService;
  private final SecureRandom random = new SecureRandom();

  public record PendingRegistration(
      String otp,
      String fullName,
      String email,
      String password,
      Instant expiry,
      int failedAttempts) {}

  public record PendingPasswordReset(
      String otp, String email, Instant expiry, int failedAttempts) {}

  private final Map<String, PendingRegistration> registrations = new ConcurrentHashMap<>();
  private final Map<String, PendingPasswordReset> passwordResets = new ConcurrentHashMap<>();

  public OtpService(EmailService emailService) {
    this.emailService = emailService;
  }

  public String generateOtp() {
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }

  public void createAndSendRegistrationOtp(String fullName, String email, String password) {
    String otp = generateOtp();
    Instant expiry = Instant.now().plusSeconds(300);
    registrations.put(
        email.toLowerCase().trim(),
        new PendingRegistration(
            otp, fullName, email.toLowerCase().trim(), password, expiry, 0));
    emailService.sendOtpEmail(email.toLowerCase().trim(), otp, "REGISTER");
  }

  public PendingRegistration getPendingRegistration(String email) {
    if (email == null) return null;
    return registrations.get(email.toLowerCase().trim());
  }

  public PendingRegistration verifyRegistrationOtp(String email, String otp) {
    if (email == null || otp == null) return null;
    String key = email.toLowerCase().trim();
    PendingRegistration pending = registrations.get(key);
    if (pending == null) {
      return null;
    }
    if (Instant.now().isAfter(pending.expiry())) {
      registrations.remove(key);
      return null;
    }
    if (!pending.otp().equals(otp.trim())) {
      int failedAttempts = pending.failedAttempts() + 1;
      if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
        registrations.remove(key);
      } else {
        registrations.put(
            key,
            new PendingRegistration(
                pending.otp(),
                pending.fullName(),
                pending.email(),
                pending.password(),
                pending.expiry(),
                failedAttempts));
      }
      return null;
    }
    registrations.remove(key);
    return pending;
  }

  public void resendRegistrationOtp(String email) {
    if (email == null) return;
    String key = email.toLowerCase().trim();
    PendingRegistration pending = registrations.get(key);
    if (pending != null) {
      String newOtp = generateOtp();
      Instant expiry = Instant.now().plusSeconds(300);
      registrations.put(
          key,
          new PendingRegistration(
              newOtp, pending.fullName(), pending.email(), pending.password(), expiry, 0));
      emailService.sendOtpEmail(key, newOtp, "REGISTER");
    }
  }

  public void createAndSendPasswordResetOtp(String email) {
    String otp = generateOtp();
    Instant expiry = Instant.now().plusSeconds(300);
    passwordResets.put(
        email.toLowerCase().trim(),
        new PendingPasswordReset(otp, email.toLowerCase().trim(), expiry, 0));
    emailService.sendOtpEmail(email.toLowerCase().trim(), otp, "RESET");
  }

  public boolean verifyPasswordResetOtp(String email, String otp) {
    if (email == null || otp == null) return false;
    String key = email.toLowerCase().trim();
    PendingPasswordReset pending = passwordResets.get(key);
    if (pending == null) {
      return false;
    }
    if (Instant.now().isAfter(pending.expiry())) {
      passwordResets.remove(key);
      return false;
    }
    if (!pending.otp().equals(otp.trim())) {
      int failedAttempts = pending.failedAttempts() + 1;
      if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
        passwordResets.remove(key);
      } else {
        passwordResets.put(
            key,
            new PendingPasswordReset(
                pending.otp(), pending.email(), pending.expiry(), failedAttempts));
      }
      return false;
    }
    passwordResets.remove(key);
    return true;
  }

  public void resendPasswordResetOtp(String email) {
    if (email == null) return;
    String key = email.toLowerCase().trim();
    createAndSendPasswordResetOtp(key);
  }
}
