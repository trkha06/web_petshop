package com.utepetshop.service;

import com.utepetshop.entity.OtpPurpose;
import com.utepetshop.entity.PendingOtp;
import com.utepetshop.repository.PendingOtpRepository;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class OtpService {
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final int MAX_SENDS_PER_HOUR = 3;
  private static final int OTP_VALIDITY_SECONDS = 300;
  private static final int RESEND_COOLDOWN_SECONDS = 60;
  private final EmailService emailService;
  private final PasswordEncoder passwordEncoder;
  private final PendingOtpRepository pendingOtpRepository;
  private final SecureRandom random = new SecureRandom();

  public record PendingRegistration(String fullName, String email, String passwordHash) {}

  public OtpService(
      EmailService emailService,
      PasswordEncoder passwordEncoder,
      PendingOtpRepository pendingOtpRepository) {
    this.emailService = emailService;
    this.passwordEncoder = passwordEncoder;
    this.pendingOtpRepository = pendingOtpRepository;
  }

  public String generateOtp() {
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }

  @Transactional
  public void createAndSendRegistrationOtp(String fullName, String email, String password) {
    issueOtp(
        cleanEmail(email),
        OtpPurpose.REGISTRATION,
        fullName == null ? "" : fullName.trim(),
        passwordEncoder.encode(password));
  }

  @Transactional
  public PendingRegistration getPendingRegistration(String email) {
    return pendingOtpRepository
        .findByEmailAndPurpose(cleanEmail(email), OtpPurpose.REGISTRATION)
        .filter(this::isUsable)
        .map(this::toPendingRegistration)
        .orElse(null);
  }

  @Transactional
  public PendingRegistration verifyRegistrationOtp(String email, String otp) {
    PendingOtp pending = consumeOtp(cleanEmail(email), OtpPurpose.REGISTRATION, otp);
    return pending == null ? null : toPendingRegistration(pending);
  }

  @Transactional
  public void resendRegistrationOtp(String email) {
    String cleanEmail = cleanEmail(email);
    ensurePendingRequestExists(cleanEmail, OtpPurpose.REGISTRATION);
    issueOtp(cleanEmail, OtpPurpose.REGISTRATION, null, null);
  }

  @Transactional
  public void createAndSendPasswordResetOtp(String email) {
    issueOtp(cleanEmail(email), OtpPurpose.PASSWORD_RESET, null, null);
  }

  @Transactional
  public boolean verifyPasswordResetOtp(String email, String otp) {
    return consumeOtp(cleanEmail(email), OtpPurpose.PASSWORD_RESET, otp) != null;
  }

  @Transactional
  public void resendPasswordResetOtp(String email) {
    String cleanEmail = cleanEmail(email);
    ensurePendingRequestExists(cleanEmail, OtpPurpose.PASSWORD_RESET);
    issueOtp(cleanEmail, OtpPurpose.PASSWORD_RESET, null, null);
  }

  private void issueOtp(
      String email, OtpPurpose purpose, String fullName, String passwordHash) {
    LocalDateTime now = LocalDateTime.now();
    pendingOtpRepository.deleteByExpiresAtBefore(now.minusHours(1));
    PendingOtp pending =
        pendingOtpRepository.findByEmailAndPurposeForUpdate(email, purpose).orElse(null);

    if (pending != null && now.isBefore(pending.getLastSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS))) {
      throw new IllegalStateException("Vui lòng chờ 60 giây trước khi yêu cầu gửi lại OTP");
    }

    int sendCount = 0;
    LocalDateTime sendWindowStartedAt = now;
    if (pending != null && now.isBefore(pending.getSendWindowStartedAt().plusHours(1))) {
      sendCount = pending.getSendCount();
      sendWindowStartedAt = pending.getSendWindowStartedAt();
    }
    if (sendCount >= MAX_SENDS_PER_HOUR) {
      throw new IllegalStateException("Bạn đã yêu cầu OTP quá nhiều lần. Vui lòng thử lại sau 1 giờ");
    }

    if (pending == null) {
      pending = new PendingOtp();
      pending.setEmail(email);
      pending.setPurpose(purpose);
      pending.setCreatedAt(now);
    }
    if (fullName != null) {
      pending.setFullName(fullName);
    }
    if (passwordHash != null) {
      pending.setPasswordHash(passwordHash);
    }

    String otp = generateOtp();
    pending.setOtpHash(passwordEncoder.encode(otp));
    pending.setExpiresAt(now.plusSeconds(OTP_VALIDITY_SECONDS));
    pending.setFailedAttempts(0);
    pending.setSendCount(sendCount + 1);
    pending.setSendWindowStartedAt(sendWindowStartedAt);
    pending.setLastSentAt(now);
    pendingOtpRepository.save(pending);
    emailService.sendOtpEmail(email, otp, purpose == OtpPurpose.REGISTRATION ? "REGISTER" : "RESET");
  }

  private PendingOtp consumeOtp(String email, OtpPurpose purpose, String otp) {
    if (otp == null || otp.isBlank()) {
      return null;
    }
    PendingOtp pending =
        pendingOtpRepository.findByEmailAndPurposeForUpdate(email, purpose).orElse(null);
    if (pending == null || !isUsable(pending)) {
      return null;
    }
    if (!passwordEncoder.matches(otp.trim(), pending.getOtpHash())) {
      int failedAttempts = pending.getFailedAttempts() + 1;
      if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
        pendingOtpRepository.delete(pending);
      } else {
        pending.setFailedAttempts(failedAttempts);
        pendingOtpRepository.save(pending);
      }
      return null;
    }
    pendingOtpRepository.delete(pending);
    return pending;
  }

  private void ensurePendingRequestExists(String email, OtpPurpose purpose) {
    PendingOtp pending =
        pendingOtpRepository.findByEmailAndPurposeForUpdate(email, purpose).orElse(null);
    if (pending == null || !isUsable(pending)) {
      throw new IllegalStateException("Yêu cầu OTP đã hết hạn. Vui lòng thực hiện lại từ đầu");
    }
  }

  private boolean isUsable(PendingOtp pending) {
    return LocalDateTime.now().isBefore(pending.getExpiresAt())
        && pending.getFailedAttempts() < MAX_FAILED_ATTEMPTS;
  }

  private PendingRegistration toPendingRegistration(PendingOtp pending) {
    return new PendingRegistration(pending.getFullName(), pending.getEmail(), pending.getPasswordHash());
  }

  private String cleanEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email không hợp lệ");
    }
    return email.toLowerCase().trim();
  }
}
