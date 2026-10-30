package com.utepetshop.controller.web;

import com.utepetshop.dto.ChatMessageDto;
import com.utepetshop.dto.ChatRoomDto;
import com.utepetshop.entity.RoleName;
import com.utepetshop.entity.User;
import com.utepetshop.repository.UserRepository;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    if (!isStaff) {
      String botReply = getBotReply(message.getContent().trim());
      if (botReply != null) {
        ChatMessageDto botMsg = ChatMessageDto.builder()
            .roomId(roomId)
            .senderName("Trợ Lý Ảo UTEPet")
            .senderEmail("bot@utepet.vn")
            .senderRole("BOT")
            .content(botReply)
            .time(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
            .staff(false)
            .bot(true)
            .build();

        if (messages.size() >= 100) {
          messages.removeFirst();
        }
        messages.add(botMsg);
        room.setLastMessage("🤖 " + botReply);
        room.setLastTime(botMsg.getTime());
        room.setLastUpdated(System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, botMsg);
        messagingTemplate.convertAndSend("/topic/staff.chat", room);
      }
    }
  }

  private String getBotReply(String text) {
    String lower = text.toLowerCase();
    if (lower.contains("bảng giá") || lower.contains("giá spa") || lower.contains("giá") || lower.contains("chi phí") || lower.contains("bao nhiêu tiền") || lower.contains("gói spa")) {
      return "🐾 Bảng giá dịch vụ Spa tham khảo tại UTEPet:\n"
          + "• Tắm vệ sinh cơ bản & sấy chải: từ 120.000đ - 250.000đ (theo cân nặng)\n"
          + "• Cắt tỉa lông tạo kiểu nghệ thuật: từ 200.000đ - 450.000đ\n"
          + "• Gói Spa dưỡng lông toàn diện + tắm bồn sục O2: từ 350.000đ\n"
          + "👉 Bạn có thể xem chi tiết ở bảng giá hoặc bấm Đặt Lịch để chọn gói phù hợp nhé!";
    }
    if (lower.contains("địa chỉ") || lower.contains("ở đâu") || lower.contains("giờ mở cửa") || lower.contains("giờ đón") || lower.contains("liên hệ") || lower.contains("chi nhánh")) {
      return "📍 UTEPet Care & Spa rất hân hạnh được đón tiếp bạn và bé tại:\n"
          + "• Trụ sở chính: 01 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh\n"
          + "• Thời gian đón khách: 08:00 - 21:00 hàng ngày (kể cả Thứ Bảy, Chủ Nhật & Ngày Lễ)\n"
          + "• Bạn có thể gửi câu hỏi trực tiếp tại khung trò chuyện này.";
    }
    if (lower.contains("đặt lịch") || lower.contains("hẹn") || lower.contains("booking") || lower.contains("cách đặt")) {
      return "📅 Bạn có thể đặt lịch hẹn Spa nhanh chóng qua 3 bước:\n"
          + "1. Bấm nút 'Đặt Lịch Ngay' trên website hoặc menu Đặt Lịch\n"
          + "2. Chọn hồ sơ bé cưng, dịch vụ và khung giờ mong muốn\n"
          + "3. Xác nhận và nhận mã lịch hẹn ngay lập tức!\n"
          + "👉 Hãy bấm vào nút 'Đặt Lịch Ngay Hôm Nay 🐾' để chọn giờ nhé!";
    }
    if (lower.contains("hủy lịch") || lower.contains("đổi lịch") || lower.contains("chính sách")) {
      return "📋 Chính sách lịch hẹn tại UTEPet:\n"
          + "• Đổi khung giờ: Miễn phí trước 2 tiếng so với giờ hẹn\n"
          + "• Hủy lịch: Bạn có thể vào mục 'Lịch hẹn đã đặt' trong tài khoản để hủy mà không mất phí\n"
          + "• Đón bé trễ: Vui lòng gửi tin nhắn tại khung trò chuyện để nhân viên hỗ trợ giữ chỗ.";
    }
    if (lower.contains("nhân viên") || lower.contains("gặp nhân viên") || lower.contains("tư vấn viên") || lower.contains("người thật") || lower.contains("staff")) {
      return "🩺 Dạ, em đã chuyển thông báo đến chuyên viên tư vấn UTEPet! Nhân viên trực sẽ đọc tin nhắn và phản hồi trực tiếp với bạn ngay trong giây lát. Bạn vui lòng để lại câu hỏi cụ thể nhé!";
    }
    return null;
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
