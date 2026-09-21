// File: src/main/java/com/utepetshop/config/WebSocketAuthInterceptor.java
package com.utepetshop.config;

import com.utepetshop.security.CustomUserDetailsService;
import com.utepetshop.security.JwtService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {
  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      Principal existingUser = accessor.getUser();

      // Nếu người dùng đã xác thực qua HttpSession (Thymeleaf web)
      if (existingUser != null) {
        log.info("WebSocket connection authenticated via Session: {}", existingUser.getName());
        return message;
      }

      // Trích xuất Authorization: Bearer <jwt> từ STOMP CONNECT header cho REST client/Postman
      String authHeader = accessor.getFirstNativeHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        try {
          String email = jwtService.email(token);
          UserDetails userDetails = userDetailsService.loadUserByUsername(email);
          UsernamePasswordAuthenticationToken auth =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          accessor.setUser(auth);
          log.info("WebSocket connection authenticated via JWT token: {}", email);
        } catch (Exception e) {
          log.warn("Failed to authenticate STOMP connection with JWT: {}", e.getMessage());
        }
      }
    }
    return message;
  }
}
