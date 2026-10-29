package com.utepetshop.controller.api;

import com.utepetshop.dto.*;
import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.security.JwtService;
import com.utepetshop.service.OtpService;
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
  private final OtpService otpService;

  public AuthApiController(
      AuthenticationConfiguration c,
      JwtService jwt,
      UserRepository users,
      RoleRepository roles,
      PasswordEncoder encoder,
      OtpService otpService)
      throws Exception {
    this.auth = c.getAuthenticationManager();
    this.jwt = jwt;
    this.users = users;
    this.roles = roles;
    this.encoder = encoder;
    this.otpService = otpService;
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
    String email = req.email().toLowerCase().trim();
    if (users.findByEmail(email).isPresent())
      return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng"));

    otpService.createAndSendRegistrationOtp(req.fullName().trim(), email, req.password());
    return ResponseEntity.accepted()
        .body(Map.of("message", "Mã OTP đã được gửi đến email đăng ký"));
  }

  @PostMapping("/register/verify")
  ResponseEntity<?> verifyRegistration(@Valid @RequestBody RegistrationOtpRequest req) {
    OtpService.PendingRegistration pending =
        otpService.verifyRegistrationOtp(req.email(), req.otp());
    if (pending == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("message", "Mã OTP không đúng, đã hết hạn hoặc vượt quá số lần thử"));
    }
    if (users.findByEmail(pending.email()).isPresent()) {
      return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng"));
    }

    User u = new User();
    u.setFullName(pending.fullName());
    u.setEmail(pending.email());
    u.setPassword(encoder.encode(pending.password()));
    u.getRoles().add(roles.findByName(RoleName.CUSTOMER).orElseThrow());
    users.save(u);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "Đăng ký thành công", "token", jwt.create(u)));
  }
}
