package com.utepetshop.controller.web;

import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {
  private final UserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder encoder;

  public RegistrationController(UserRepository u, RoleRepository r, PasswordEncoder e) {
    users = u;
    roles = r;
    encoder = e;
  }

  @GetMapping("/register")
  String page() {
    return "auth/register";
  }

  @PostMapping("/register")
  String register(
      @RequestParam String fullName,
      @RequestParam String email,
      @RequestParam String password,
      RedirectAttributes ra) {
    if (users.findByEmail(email).isPresent()) {
      ra.addFlashAttribute("error", "Email đã được sử dụng");
      return "redirect:/register";
    }
    if (password.length() < 8) {
      ra.addFlashAttribute("error", "Mật khẩu cần tối thiểu 8 ký tự");
      return "redirect:/register";
    }
    User u = new User();
    u.setFullName(fullName);
    u.setEmail(email);
    u.setPassword(encoder.encode(password));
    u.getRoles().add(roles.findByName(RoleName.CUSTOMER).orElseThrow());
    users.save(u);
    ra.addFlashAttribute("success", "Đăng ký thành công, hãy đăng nhập");
    return "redirect:/login";
  }
}
