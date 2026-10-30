package com.utepetshop.repository;

import com.utepetshop.entity.OtpPurpose;
import com.utepetshop.entity.PendingOtp;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PendingOtpRepository extends JpaRepository<PendingOtp, Long> {
  Optional<PendingOtp> findByEmailAndPurpose(String email, OtpPurpose purpose);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT otp FROM PendingOtp otp WHERE otp.email = :email AND otp.purpose = :purpose")
  Optional<PendingOtp> findByEmailAndPurposeForUpdate(
      @Param("email") String email, @Param("purpose") OtpPurpose purpose);

  long deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
