package com.utepetshop.config;

import com.utepetshop.security.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
    return http.csrf(c -> c.ignoringRequestMatchers("/api/**"))
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/",
                        "/products/**",
                        "/login",
                        "/register",
                        "/css/**",
                        "/api/auth/**",
                        "/api/products/**")
                    .permitAll()
                    .requestMatchers("/manager/**", "/api/admin/**")
                    .hasAnyRole("MANAGER", "ADMIN")
                    .requestMatchers("/staff/**")
                    .hasAnyRole("STAFF", "MANAGER", "ADMIN")
                    .requestMatchers("/cart/**", "/checkout/**", "/orders/**", "/api/orders/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/products", true).permitAll())
        .logout(l -> l.logoutSuccessUrl("/products"))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
