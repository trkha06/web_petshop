package com.utepetshop.controller.web;

import com.utepetshop.entity.User;
import com.utepetshop.repository.UserRepository;
import com.utepetshop.service.OtpService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final OtpService otpService;

  public ForgotPasswordController(
      UserRepository users, PasswordEncoder encoder, OtpService otpService) {
    this.users = users;
    this.encoder = encoder;
    this.otpService = otpService;
  }

  @GetMapping("/forgot-password")
  String forgotPasswordPage() {
    return "auth/forgot-password";
  }

  @PostMapping("/forgot-password")
  String sendResetOtp(@RequestParam String email, RedirectAttributes ra) {
    String cleanEmail = email.toLowerCase().trim();
    if (users.findByEmail(cleanEmail).isEmpty()) {
      ra.addFlashAttribute("error", "Email này chưa được đăng ký tài khoản trong hệ thống!");
      return "redirect:/forgot-password";
    }

    try {
      otpService.createAndSendPasswordResetOtp(cleanEmail);
      ra.addFlashAttribute(
          "success",
          "Mã OTP khôi phục mật khẩu đã được gửi đến email " + cleanEmail + ". Vui lòng kiểm tra hộp thư!");
      return "redirect:/forgot-password/reset?email="
          + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8);
    } catch (Exception ex) {
      ra.addFlashAttribute("error", "Không thể gửi email OTP. Vui lòng kiểm tra lại email hoặc thử lại sau!");
      return "redirect:/forgot-password";
    }
  }

  @GetMapping("/forgot-password/reset")
  String resetPasswordPage(
      @RequestParam(required = false) String email, Model model, RedirectAttributes ra) {
    if (email == null || email.isBlank()) {
      return "redirect:/forgot-password";
    }
    model.addAttribute("email", email);
    return "auth/reset-password";
  }

  @PostMapping("/forgot-password/reset")
  String handlePasswordReset(
      @RequestParam String email,
      @RequestParam String otp,
      @RequestParam String newPassword,
      @RequestParam String confirmPassword,
      RedirectAttributes ra) {
    String cleanEmail = email.toLowerCase().trim();

    if (newPassword.length() < 8) {
      ra.addFlashAttribute("error", "Mật khẩu mới phải có tối thiểu 8 ký tự!");
      return "redirect:/forgot-password/reset?email="
          + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8);
    }

    if (!newPassword.equals(confirmPassword)) {
      ra.addFlashAttribute("error", "Xác nhận mật khẩu mới không khớp!");
      return "redirect:/forgot-password/reset?email="
          + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8);
    }

    boolean validOtp = otpService.verifyPasswordResetOtp(cleanEmail, otp);
    if (!validOtp) {
      ra.addFlashAttribute(
          "error", "Mã OTP không chính xác hoặc đã hết thời gian hiệu lực (5 phút)!");
      return "redirect:/forgot-password/reset?email="
          + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8);
    }

    User user =
        users
            .findByEmail(cleanEmail)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản với email này"));
    user.setPassword(encoder.encode(newPassword));
    users.save(user);

    ra.addFlashAttribute("success", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.");
    return "redirect:/login";
  }

  @PostMapping("/forgot-password/resend-otp")
  String resendOtp(@RequestParam String email, RedirectAttributes ra) {
    String cleanEmail = email.toLowerCase().trim();
    try {
      otpService.resendPasswordResetOtp(cleanEmail);
      ra.addFlashAttribute("success", "Mã OTP mới đã được gửi đến email " + cleanEmail);
    } catch (Exception ex) {
      ra.addFlashAttribute("error", "Gửi lại mã OTP thất bại: " + ex.getMessage());
    }

    return "redirect:/forgot-password/reset?email="
        + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8);
  }
}
