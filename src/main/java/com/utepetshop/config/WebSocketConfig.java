// File: src/main/java/com/utepetshop/config/WebSocketConfig.java
package com.utepetshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final WebSocketAuthInterceptor authInterceptor;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Endpoint /ws hỗ trợ SockJS
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // Enable simple broker /topic và /queue
    registry.enableSimpleBroker("/topic", "/queue");
    // Application destination prefix /app
    registry.setApplicationDestinationPrefixes("/app");
    // User destination prefix /user
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // Đăng ký ChannelInterceptor xác thực JWT & Session
    registration.interceptors(authInterceptor);
  }
}
