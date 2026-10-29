package com.utepetshop.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final CustomUserDetailsService details;

  public JwtAuthFilter(JwtService jwt, CustomUserDetailsService details) {
    this.jwt = jwt;
    this.details = details;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, java.io.IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        String email = jwt.email(header.substring(7));
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
          UserDetails u = details.loadUserByUsername(email);
          if (u.isEnabled()
              && u.isAccountNonExpired()
              && u.isAccountNonLocked()
              && u.isCredentialsNonExpired()) {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
          }
        }
      } catch (JwtException | IllegalArgumentException ignored) {
      }
    }
    chain.doFilter(request, response);
  }
}
