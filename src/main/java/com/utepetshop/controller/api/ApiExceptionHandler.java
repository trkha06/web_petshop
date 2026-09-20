package com.utepetshop.controller.api;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages = "com.utepetshop.controller.api")
public class ApiExceptionHandler {
  @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class})
  ResponseEntity<?> bad(RuntimeException e) {
    return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<?> denied() {
    return ResponseEntity.status(403)
        .body(Map.of("message", "Bạn không có quyền thực hiện thao tác này"));
  }
}
