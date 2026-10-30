package com.utepetshop.controller.web;

import com.utepetshop.dto.ChatMessageDto;
import com.utepetshop.dto.ChatRoomDto;
import com.utepetshop.entity.RoleName;
import com.utepetshop.entity.User;
import com.utepetshop.repository.UserRepository;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ChatController {
  private final SimpMessagingTemplate messagingTemplate;
  private final UserRepository userRepository;

  private final Map<String, List<ChatMessageDto>> roomMessages = new ConcurrentHashMap<>();
  private final Map<String, ChatRoomDto> activeRooms = new ConcurrentHashMap<>();

  @MessageMapping("/chat.send")
  public void sendMessage(@Payload ChatMessageDto message, Principal principal) {
    if (message.getContent() == null || message.getContent().trim().isEmpty()) {
      return;
    }

    String roomId = message.getRoomId();
    if (roomId == null || roomId.trim().isEmpty()) {
      roomId = "guest_default";
    }
    roomId = roomId.trim();

    String senderName = message.getSenderName() != null && !message.getSenderName().isBlank()
        ? message.getSenderName().trim()
        : "Khách";
    String senderEmail = "";
    String senderRole = "CUSTOMER";
    boolean isStaff = false;

    if (principal != null && principal.getName() != null) {
      senderEmail = principal.getName();
      Optional<User> userOpt = userRepository.findByEmail(senderEmail);
      if (userOpt.isPresent()) {
        User u = userOpt.get();
        if (u.getFullName() != null && !u.getFullName().isBlank()) {
          senderName = u.getFullName();
        } else {
          senderName = u.getEmail();
        }
        isStaff = u.getRoles().stream().anyMatch(r ->
            r.getName() == RoleName.STAFF || r.getName() == RoleName.MANAGER || r.getName() == RoleName.ADMIN);
        senderRole = isStaff ? "STAFF" : "CUSTOMER";
      }
    }

    ChatMessageDto enriched = ChatMessageDto.of(roomId, senderName, senderEmail, senderRole, message.getContent().trim());

    List<ChatMessageDto> messages = roomMessages.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
    if (messages.size() >= 100) {
      messages.removeFirst();
    }
    messages.add(enriched);

    ChatRoomDto room = activeRooms.get(roomId);
    if (room == null) {
      String custName = isStaff ? ("Khách (" + roomId + ")") : senderName;
      String custEmail = isStaff ? "" : senderEmail;
      room = ChatRoomDto.builder()
          .roomId(roomId)
          .customerName(custName)
          .customerEmail(custEmail)
          .unreadCount(isStaff ? 0 : 1)
          .build();
    } else {
      if (!isStaff) {
        room.setCustomerName(senderName);
        if (!senderEmail.isBlank()) {
          room.setCustomerEmail(senderEmail);
        }
        room.setUnreadCount(room.getUnreadCount() + 1);
      }
    }
    room.setLastMessage(enriched.getContent());
    room.setLastTime(enriched.getTime());
    room.setLastUpdated(System.currentTimeMillis());
    activeRooms.put(roomId, room);

    messagingTemplate.convertAndSend("/topic/chat/" + roomId, enriched);
    messagingTemplate.convertAndSend("/topic/staff.chat", room);
  }

  @GetMapping("/api/chat/history")
  @ResponseBody
  public List<ChatMessageDto> getHistory(@RequestParam(value = "roomId", required = false) String roomId) {
    if (roomId == null || roomId.trim().isEmpty()) {
      return Collections.emptyList();
    }
    List<ChatMessageDto> list = roomMessages.get(roomId.trim());
    return list != null ? list : Collections.emptyList();
  }

  @GetMapping("/api/chat/rooms")
  @ResponseBody
  public List<ChatRoomDto> getRooms() {
    List<ChatRoomDto> rooms = new ArrayList<>(activeRooms.values());
    rooms.sort((a, b) -> Long.compare(b.getLastUpdated(), a.getLastUpdated()));
    return rooms;
  }

  @PostMapping("/api/chat/rooms/{roomId}/read")
  @ResponseBody
  public Map<String, Object> markAsRead(@PathVariable("roomId") String roomId) {
    ChatRoomDto room = activeRooms.get(roomId);
    if (room != null) {
      room.setUnreadCount(0);
      messagingTemplate.convertAndSend("/topic/staff.chat", room);
    }
    return Map.of("success", true);
  }
}
