package com.utepetshop.controller.web;

import com.utepetshop.dto.ChatMessageDto;
import com.utepetshop.dto.ChatRoomDto;
import com.utepetshop.entity.Branch;
import com.utepetshop.entity.ChatMessage;
import com.utepetshop.entity.ChatRoom;
import com.utepetshop.entity.RoleName;
import com.utepetshop.entity.ServiceSlot;
import com.utepetshop.entity.User;
import com.utepetshop.repository.BranchRepository;
import com.utepetshop.repository.ChatMessageRepository;
import com.utepetshop.repository.ChatRoomRepository;
import com.utepetshop.repository.ServiceRepository;
import com.utepetshop.repository.ServiceSlotRepository;
import com.utepetshop.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
  private final ServiceRepository serviceRepository;
  private final BranchRepository branchRepository;
  private final ServiceSlotRepository serviceSlotRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;

  @MessageMapping("/chat.send")
  @Transactional
  public void sendMessage(@Payload ChatMessageDto message, Principal principal) {
    User sender = requireActiveUser(principal);
    String content = normalizeContent(message.getContent());
    boolean supportUser = isSupportUser(sender);
    ChatRoom room = supportUser ? findRoom(message.getRoomId()) : getOrCreateCustomerRoom(sender);

    ChatMessage saved = saveMessage(room, sender, supportUser ? "STAFF" : "CUSTOMER", content);
    room.setLastMessage(content);
    room.setLastMessageAt(saved.getCreatedAt());
    if (!supportUser) {
      room.setUnreadCount(room.getUnreadCount() + 1);
    }
    chatRoomRepository.save(room);

    messagingTemplate.convertAndSend("/topic/chat/" + room.getRoomId(), toMessageDto(saved));
    messagingTemplate.convertAndSend("/topic/staff.chat", toRoomDto(room));

    if (!supportUser) {
      String botReply = getBotReply(content);
      if (botReply != null) {
        ChatMessage botMessage = saveMessage(room, null, "BOT", botReply);
        messagingTemplate.convertAndSend("/topic/chat/" + room.getRoomId(), toMessageDto(botMessage));
      }
    }
  }

  private String getBotReply(String text) {
    String lower = text.toLowerCase(Locale.ROOT);
    if (lower.contains("bảng giá") || lower.contains("giá spa") || lower.contains("giá") || lower.contains("chi phí") || lower.contains("bao nhiêu tiền") || lower.contains("gói spa")) {
      return buildServicePriceReply();
    }
    if (lower.contains("địa chỉ") || lower.contains("ở đâu") || lower.contains("giờ mở cửa") || lower.contains("giờ đón") || lower.contains("liên hệ") || lower.contains("chi nhánh")) {
      return buildBranchAndScheduleReply();
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

  private String buildServicePriceReply() {
    List<com.utepetshop.entity.Service> services = serviceRepository.findByActiveTrueOrderByIdAsc();
    if (services.isEmpty()) {
      return "🐾 Hiện chưa có dịch vụ đang hoạt động. Bạn vui lòng quay lại sau.";
    }

    StringBuilder reply = new StringBuilder("🐾 Bảng giá dịch vụ hiện có tại UTEPet:\n");
    for (com.utepetshop.entity.Service service : services) {
      reply.append("• ")
          .append(service.getName())
          .append(": ")
          .append(formatPrice(service.getPrice()));
      if (service.getDurationMinutes() > 0) {
        reply.append(" · ").append(service.getDurationMinutes()).append(" phút");
      }
      reply.append("\n");
    }
    reply.append("👉 Bạn có thể chọn dịch vụ và khung giờ phù hợp tại trang Đặt lịch.");
    return reply.toString();
  }

  private String buildBranchAndScheduleReply() {
    List<Branch> branches = branchRepository.findByActiveTrueOrderByNameAsc();
    StringBuilder reply = new StringBuilder("📍 Các chi nhánh đang hoạt động:\n");
    if (branches.isEmpty()) {
      reply.append("• Hiện chưa có chi nhánh đang hoạt động.\n");
    } else {
      for (Branch branch : branches) {
        reply.append("• ")
            .append(branch.getName())
            .append(": ")
            .append(branch.getAddress())
            .append("\n");
      }
    }

    List<ServiceSlot> slots =
        serviceSlotRepository.findNextActiveSlots(LocalDate.now());
    if (slots.isEmpty()) {
      reply.append("🕒 Hiện chưa có khung giờ dịch vụ được mở.");
    } else {
      ServiceSlot slot = slots.getFirst();
      reply.append("🕒 Khung giờ dịch vụ gần nhất: ")
          .append(slot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
          .append(" - ")
          .append(slot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
          .append(" ngày ")
          .append(slot.getSlotDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
          .append(" tại ")
          .append(slot.getBranch().getName())
          .append(".");
    }
    return reply.toString();
  }

  private String formatPrice(BigDecimal price) {
    if (price == null) {
      return "Liên hệ";
    }
    return java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(price)
        + "đ";
  }

  @GetMapping("/api/chat/history")
  @ResponseBody
  @Transactional
  public List<ChatMessageDto> getHistory(
      @RequestParam(value = "roomId", required = false) String roomId, Principal principal) {
    ChatRoom room = findAccessibleRoom(roomId, requireActiveUser(principal));
    return chatMessageRepository.findByRoomRoomIdOrderByCreatedAtAsc(room.getRoomId()).stream()
        .map(this::toMessageDto)
        .toList();
  }

  @GetMapping("/api/chat/my-room")
  @ResponseBody
  @Transactional
  public ChatRoomDto getMyRoom(Principal principal) {
    User user = requireActiveUser(principal);
    if (isSupportUser(user)) {
      throw new AccessDeniedException("Tài khoản hỗ trợ không có phòng chat khách hàng riêng");
    }
    return toRoomDto(getOrCreateCustomerRoom(user));
  }

  @GetMapping("/api/chat/rooms")
  @ResponseBody
  @Transactional
  public List<ChatRoomDto> getRooms(Principal principal) {
    requireSupportRole(requireActiveUser(principal));
    return chatRoomRepository.findAllByOrderByLastMessageAtDesc().stream()
        .map(this::toRoomDto)
        .toList();
  }

  @PostMapping("/api/chat/rooms/{roomId}/read")
  @ResponseBody
  @Transactional
  public Map<String, Object> markAsRead(@PathVariable("roomId") String roomId, Principal principal) {
    requireSupportRole(requireActiveUser(principal));
    ChatRoom room = findRoom(roomId);
    room.setUnreadCount(0);
    chatRoomRepository.save(room);
    messagingTemplate.convertAndSend("/topic/staff.chat", toRoomDto(room));
    return Map.of("success", true);
  }

  private ChatRoom getOrCreateCustomerRoom(User customer) {
    return chatRoomRepository
        .findByCustomerId(customer.getId())
        .orElseGet(
            () -> {
              ChatRoom room = new ChatRoom();
              room.setRoomId(UUID.randomUUID().toString());
              room.setCustomer(customer);
              room.setUnreadCount(0);
              return chatRoomRepository.save(room);
            });
  }

  private ChatRoom findAccessibleRoom(String roomId, User user) {
    ChatRoom room = findRoom(roomId);
    if (!isSupportUser(user) && !room.getCustomer().getId().equals(user.getId())) {
      throw new AccessDeniedException("Bạn không có quyền xem phòng chat này");
    }
    return room;
  }

  private ChatRoom findRoom(String roomId) {
    if (roomId == null || roomId.isBlank()) {
      throw new AccessDeniedException("Phòng chat không hợp lệ");
    }
    return chatRoomRepository
        .findByRoomId(roomId.trim())
        .orElseThrow(() -> new AccessDeniedException("Không tìm thấy phòng chat"));
  }

  private ChatMessage saveMessage(ChatRoom room, User sender, String senderRole, String content) {
    ChatMessage chatMessage = new ChatMessage();
    chatMessage.setRoom(room);
    chatMessage.setSender(sender);
    chatMessage.setSenderName(sender == null ? "Trợ Lý Ảo UTEPet" : displayName(sender));
    chatMessage.setSenderEmail(sender == null ? "bot@utepet.vn" : sender.getEmail());
    chatMessage.setSenderRole(senderRole);
    chatMessage.setContent(content);
    chatMessage.setCreatedAt(LocalDateTime.now());
    return chatMessageRepository.save(chatMessage);
  }

  private ChatMessageDto toMessageDto(ChatMessage message) {
    String senderRole = message.getSenderRole();
    return ChatMessageDto.builder()
        .roomId(message.getRoom().getRoomId())
        .senderName(message.getSenderName())
        .senderEmail(message.getSenderEmail())
        .senderRole(senderRole)
        .content(message.getContent())
        .time(message.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
        .staff("STAFF".equals(senderRole))
        .bot("BOT".equals(senderRole))
        .build();
  }

  private ChatRoomDto toRoomDto(ChatRoom room) {
    User customer = room.getCustomer();
    LocalDateTime lastMessageAt = room.getLastMessageAt();
    return ChatRoomDto.builder()
        .roomId(room.getRoomId())
        .customerName(displayName(customer))
        .customerEmail(customer.getEmail())
        .lastMessage(room.getLastMessage())
        .lastTime(lastMessageAt == null ? "" : lastMessageAt.format(DateTimeFormatter.ofPattern("HH:mm")))
        .lastUpdated(
            lastMessageAt == null
                ? 0L
                : lastMessageAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        .unreadCount(room.getUnreadCount())
        .build();
  }

  private User requireActiveUser(Principal principal) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new AccessDeniedException("Cần đăng nhập để sử dụng chat");
    }
    return userRepository
        .findByEmail(principal.getName())
        .filter(User::isActive)
        .orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ hoặc đã bị khóa"));
  }

  private void requireSupportRole(User user) {
    if (!isSupportUser(user)) {
      throw new AccessDeniedException("Chỉ nhân viên hỗ trợ mới được sử dụng chức năng này");
    }
  }

  private boolean isSupportUser(User user) {
    return user.getRoles().stream()
        .anyMatch(
            role ->
                role.getName() == RoleName.STAFF
                    || role.getName() == RoleName.MANAGER
                    || role.getName() == RoleName.ADMIN);
  }

  private String normalizeContent(String content) {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
    }
    String value = content.trim();
    if (value.length() > 2000) {
      throw new IllegalArgumentException("Nội dung tin nhắn không được vượt quá 2000 ký tự");
    }
    return value;
  }

  private String displayName(User user) {
    return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
  }
}
