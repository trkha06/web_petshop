package com.utepetshop.repository;

import com.utepetshop.entity.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
  Optional<ChatRoom> findByRoomId(String roomId);

  Optional<ChatRoom> findByCustomerId(Long customerId);

  boolean existsByRoomIdAndCustomerId(String roomId, Long customerId);

  List<ChatRoom> findAllByOrderByLastMessageAtDesc();
}
