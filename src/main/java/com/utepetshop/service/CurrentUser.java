package com.utepetshop.service;

import com.utepetshop.entity.User;
import com.utepetshop.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUser {
  private final UserRepository users;

  public CurrentUser(UserRepository users) {
    this.users = users;
  }

  public User required() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    return users
        .findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản"));
  }
}
