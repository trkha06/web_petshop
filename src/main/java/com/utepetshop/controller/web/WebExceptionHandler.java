package com.utepetshop.controller.web;

import com.utepetshop.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Converts web failures into the site's error pages while preserving full server-side logging. */
@ControllerAdvice(basePackages = "com.utepetshop.controller.web")
@Slf4j
public class WebExceptionHandler {

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String forbidden(AccessDeniedException ex, Model model) {
    model.addAttribute("message", ex.getMessage());
    return "error/403";
  }

  @ExceptionHandler({NoSuchElementException.class})
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String notFound(RuntimeException ex, Model model) {
    model.addAttribute("message", ex.getMessage());
    return "error/404";
  }

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String badRequest(BusinessException ex, Model model) {
    model.addAttribute("message", ex.getMessage());
    return "error/400";
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String internalError(Exception ex, HttpServletRequest request, Model model) {
    log.error("Unhandled web exception for {}", request.getRequestURI(), ex);
    model.addAttribute("message", "Đã xảy ra lỗi ngoài ý muốn. Vui lòng thử lại sau.");
    return "error/500";
  }
}
