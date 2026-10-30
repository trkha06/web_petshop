package com.utepetshop.config;

import com.utepetshop.entity.RoleName;
import com.utepetshop.entity.User;
import com.utepetshop.repository.ChatRoomRepository;
import com.utepetshop.repository.UserRepository;
import com.utepetshop.security.CustomUserDetailsService;
import com.utepetshop.security.JwtService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;
  private final UserRepository userRepository;
  private final ChatRoomRepository chatRoomRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      authenticateConnection(accessor);
      return message;
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      validateSubscription(accessor);
    }

    if (StompCommand.SEND.equals(accessor.getCommand())) {
      requireActiveUser(accessor.getUser());
      if (!"/app/chat.send".equals(accessor.getDestination())) {
        throw new AccessDeniedException("Không được phép gửi tới kênh này");
      }
    }
    return message;
  }

  private void authenticateConnection(StompHeaderAccessor accessor) {
    Principal principal = accessor.getUser();
    if (principal == null) {
      String authHeader = accessor.getFirstNativeHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String email = jwtService.email(authHeader.substring(7));
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (userDetails.isEnabled()) {
          accessor.setUser(
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities()));
          principal = accessor.getUser();
        }
      }
    }
    requireActiveUser(principal);
  }

  private void validateSubscription(StompHeaderAccessor accessor) {
    User user = requireActiveUser(accessor.getUser());
    String destination = accessor.getDestination();
    if (destination == null) {
      throw new AccessDeniedException("Kênh chat không hợp lệ");
    }
    if ("/topic/staff.chat".equals(destination)) {
      requireSupportRole(user);
      return;
    }
    if (destination.startsWith("/topic/chat/")) {
      String roomId = destination.substring("/topic/chat/".length());
      if (!isSupportUser(user) && !chatRoomRepository.existsByRoomIdAndCustomerId(roomId, user.getId())) {
        throw new AccessDeniedException("Bạn không có quyền xem phòng chat này");
      }
      return;
    }
    if (!destination.startsWith("/user/")) {
      throw new AccessDeniedException("Không được phép đăng ký kênh này");
    }
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
      throw new AccessDeniedException("Chỉ nhân viên hỗ trợ mới được xem danh sách chat");
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
}
