// File: src/main/java/com/utepetshop/repository/NotificationRepository.java
package com.utepetshop.repository;

import com.utepetshop.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

  long countByUserIdAndReadFalse(Long userId);
}
