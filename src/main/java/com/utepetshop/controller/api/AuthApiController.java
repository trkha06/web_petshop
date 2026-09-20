package com.utepetshop.controller.api;

import com.utepetshop.dto.*;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.security.JwtService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
  private final AuthenticationManager auth;
  private final JwtService jwt;
  private final UserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder encoder;

  public AuthApiController(
      AuthenticationConfiguration c,
      JwtService jwt,
      UserRepository users,
      RoleRepository roles,
      PasswordEncoder encoder)
      throws Exception {
    this.auth = c.getAuthenticationManager();
    this.jwt = jwt;
    this.users = users;
    this.roles = roles;
    this.encoder = encoder;
  }

  @PostMapping("/login")
  Map<String, Object> login(@Valid @RequestBody AuthRequest req) {
    auth.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
    User u = users.findByEmail(req.email()).orElseThrow();
    return Map.of(
        "token",
        jwt.create(u),
        "tokenType",
        "Bearer",
        "roles",
        u.getRoles().stream().map(r -> r.getName().name()).toList());
  }

  @PostMapping("/register")
  ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
    if (users.findByEmail(req.email()).isPresent())
      return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng"));
    User u = new User();
    u.setFullName(req.fullName());
    u.setEmail(req.email());
    u.setPassword(encoder.encode(req.password()));
    u.getRoles().add(roles.findByName(RoleName.CUSTOMER).orElseThrow());
    users.save(u);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "Đăng ký thành công", "token", jwt.create(u)));
  }
}
