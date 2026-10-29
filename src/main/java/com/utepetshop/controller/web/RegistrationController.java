package com.utepetshop.controller.web;

import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.service.OtpService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {
  private final UserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder encoder;
  private final OtpService otpService;

  public RegistrationController(
      UserRepository u, RoleRepository r, PasswordEncoder e, OtpService otpService) {
    users = u;
    roles = r;
    encoder = e;
    this.otpService = otpService;
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
    if (users.findByEmail(email.toLowerCase().trim()).isPresent()) {
      ra.addFlashAttribute("error", "Email này đã được đăng ký tài khoản");
      return "redirect:/register";
    }
    if (password.length() < 8) {
      ra.addFlashAttribute("error", "Mật khẩu cần tối thiểu 8 ký tự");
      return "redirect:/register";
    }

    try {
      otpService.createAndSendRegistrationOtp(fullName, email, password);
      ra.addFlashAttribute(
          "success", "Mã xác thực OTP đã được gửi đến email " + email + ". Vui lòng kiểm tra hộp thư!");
      return "redirect:/register/verify-otp?email="
          + URLEncoder.encode(email.toLowerCase().trim(), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      ra.addFlashAttribute(
          "error", "Không thể gửi email OTP. Vui lòng kiểm tra lại địa chỉ email hoặc thử lại sau!");
      return "redirect:/register";
    }
  }

  @GetMapping("/register/verify-otp")
  String verifyOtpPage(@RequestParam(required = false) String email, Model model, RedirectAttributes ra) {
    if (email == null || email.isBlank()) {
      return "redirect:/register";
    }
    var pending = otpService.getPendingRegistration(email);
    if (pending == null) {
      ra.addFlashAttribute("error", "Yêu cầu đăng ký đã hết hạn. Vui lòng đăng ký lại!");
      return "redirect:/register";
    }
    model.addAttribute("email", email);
    return "auth/verify-register-otp";
  }

  @PostMapping("/register/verify-otp")
  String verifyOtp(
      @RequestParam String email,
      @RequestParam String otp,
      RedirectAttributes ra) {
    var pending = otpService.verifyRegistrationOtp(email, otp);
    if (pending == null) {
      ra.addFlashAttribute("error", "Mã OTP không chính xác hoặc đã hết thời gian hiệu lực (5 phút)");
      return "redirect:/register/verify-otp?email="
          + URLEncoder.encode(email.toLowerCase().trim(), StandardCharsets.UTF_8);
    }

    if (users.findByEmail(pending.email()).isPresent()) {
      ra.addFlashAttribute("error", "Tài khoản với email này đã tồn tại");
      return "redirect:/login";
    }

    User u = new User();
    u.setFullName(pending.fullName());
    u.setEmail(pending.email());
    u.setPassword(encoder.encode(pending.password()));
    u.getRoles().add(roles.findByName(RoleName.CUSTOMER).orElseThrow());
    users.save(u);

    ra.addFlashAttribute("success", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
    return "redirect:/login";
  }

  @PostMapping("/register/resend-otp")
  String resendOtp(@RequestParam String email, RedirectAttributes ra) {
    var pending = otpService.getPendingRegistration(email);
    if (pending == null) {
      ra.addFlashAttribute("error", "Yêu cầu đăng ký đã hết hạn. Vui lòng thực hiện lại từ đầu!");
      return "redirect:/register";
    }

    try {
      otpService.resendRegistrationOtp(email);
      ra.addFlashAttribute("success", "Mã OTP mới đã được gửi đến email " + email);
    } catch (Exception ex) {
      ra.addFlashAttribute("error", "Gửi lại OTP thất bại: " + ex.getMessage());
    }

    return "redirect:/register/verify-otp?email="
        + URLEncoder.encode(email.toLowerCase().trim(), StandardCharsets.UTF_8);
  }
}
