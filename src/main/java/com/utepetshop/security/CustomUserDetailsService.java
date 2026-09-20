package com.utepetshop.security;

import com.utepetshop.entity.User;
import com.utepetshop.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository users;

  public CustomUserDetailsService(UserRepository users) {
    this.users = users;
  }

  public UserDetails loadUserByUsername(String email) {
    User u =
        users
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Email không tồn tại"));
    return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
        .password(u.getPassword())
        .authorities(
            u.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .toList())
        .disabled(!u.isActive())
        .build();
  }
}
