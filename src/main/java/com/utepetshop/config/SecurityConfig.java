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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
    AccessDeniedHandlerImpl denied = new AccessDeniedHandlerImpl();
    denied.setErrorPage("/access-denied");
    return http.csrf(c -> c.ignoringRequestMatchers("/api/**"))
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**"))
                    .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new NegatedRequestMatcher(new AntPathRequestMatcher("/api/**")))
                    .accessDeniedHandler(denied))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/",
                        "/products/**",
                        "/services/**",
                        "/login",
                        "/register",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/ws/**",
                        "/api/auth/**",
                        "/api/products/**",
                        "/api/services/**",
                        "/api/branches/**")
                    .permitAll()
                    .requestMatchers("/api/promotions/apply")
                    .authenticated()
                    .requestMatchers("/manager/**", "/api/admin/**", "/api/promotions/**")
                    .hasAnyRole("MANAGER", "ADMIN")
                    .requestMatchers("/staff/**")
                    .hasAnyRole("STAFF", "MANAGER", "ADMIN")
                    .requestMatchers("/cart/**", "/checkout/**", "/orders/**", "/api/orders/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/products").permitAll())
        .logout(l -> l.logoutSuccessUrl("/products"))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
