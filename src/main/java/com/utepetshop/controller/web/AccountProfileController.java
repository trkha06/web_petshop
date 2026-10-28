package com.utepetshop.controller.web;

import com.utepetshop.entity.User;
import com.utepetshop.repository.AddressRepository;
import com.utepetshop.service.AccountProfileService;
import com.utepetshop.service.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountProfileController {
  private final CurrentUser currentUser;
  private final AddressRepository addresses;
  private final AccountProfileService profiles;

  public AccountProfileController(
      CurrentUser currentUser, AddressRepository addresses, AccountProfileService profiles) {
    this.currentUser = currentUser;
    this.addresses = addresses;
    this.profiles = profiles;
  }

  @GetMapping("/profile")
  String profile(Model model) {
    User user = currentUser.required();
    model.addAttribute("account", user);
    model.addAttribute(
        "addresses", addresses.findByUserIdOrderByDefaultAddressDesc(user.getId()));
    return "account/profile";
  }

  @PostMapping("/profile")
  String updateProfile(
      @RequestParam String fullName,
      @RequestParam(required = false) String phone,
      RedirectAttributes redirect) {
    try {
      profiles.updateProfile(currentUser.required(), fullName, phone);
      redirect.addFlashAttribute("success", "Đã cập nhật thông tin tài khoản");
    } catch (IllegalArgumentException exception) {
      redirect.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/account/profile";
  }

  @PostMapping("/password")
  String changePassword(
      @RequestParam String currentPassword,
      @RequestParam String newPassword,
      @RequestParam String confirmPassword,
      RedirectAttributes redirect) {
    try {
      profiles.changePassword(
          currentUser.required(), currentPassword, newPassword, confirmPassword);
      redirect.addFlashAttribute("success", "Đã đổi mật khẩu thành công");
    } catch (IllegalArgumentException exception) {
      redirect.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/account/profile";
  }

  @PostMapping("/addresses")
  String addAddress(
      @RequestParam String recipientName,
      @RequestParam String phone,
      @RequestParam String line1,
      @RequestParam(required = false) String ward,
      @RequestParam(required = false) String district,
      @RequestParam String city,
      @RequestParam(defaultValue = "false") boolean defaultAddress,
      RedirectAttributes redirect) {
    try {
      profiles.addAddress(
          currentUser.required(),
          recipientName,
          phone,
          line1,
          ward,
          district,
          city,
          defaultAddress);
      redirect.addFlashAttribute("success", "Đã thêm địa chỉ giao hàng");
    } catch (IllegalArgumentException exception) {
      redirect.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/account/profile";
  }

  @PostMapping("/addresses/{id}/default")
  String setDefaultAddress(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      profiles.setDefaultAddress(currentUser.required(), id);
      redirect.addFlashAttribute("success", "Đã đặt làm địa chỉ mặc định");
    } catch (IllegalArgumentException exception) {
      redirect.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/account/profile";
  }

  @PostMapping("/addresses/{id}/delete")
  String deleteAddress(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      profiles.deleteAddress(currentUser.required(), id);
      redirect.addFlashAttribute("success", "Đã xóa địa chỉ giao hàng");
    } catch (IllegalArgumentException exception) {
      redirect.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/account/profile";
  }
}
