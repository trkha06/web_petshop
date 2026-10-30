package com.utepetshop.repository;

import com.utepetshop.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findByRoomRoomIdOrderByCreatedAtAsc(String roomId);
}
