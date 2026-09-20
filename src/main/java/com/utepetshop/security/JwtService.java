package com.utepetshop.security;

import com.utepetshop.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long minutes;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long minutes) {
    this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    this.minutes = minutes;
  }

  public String create(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getEmail())
        .claim("userId", user.getId())
        .claim("roles", user.getRoles().stream().map(r -> r.getName().name()).toList())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(Duration.ofMinutes(minutes))))
        .signWith(key)
        .compact();
  }

  public String email(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }
}
