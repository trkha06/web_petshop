package com.utepetshop.config;

import com.utepetshop.security.CustomUserDetailsService;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationConfig {
  @Bean
  AuthenticationProvider provider(CustomUserDetailsService users, PasswordEncoder encoder) {
    DaoAuthenticationProvider p = new DaoAuthenticationProvider();
    p.setUserDetailsService(users);
    p.setPasswordEncoder(encoder);
    return p;
  }
}
