package com.utepetshop.controller.api;

import com.utepetshop.entity.*;
import com.utepetshop.repository.*;
import com.utepetshop.service.CurrentUser;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountApiController {
  private final CurrentUser current;
  private final AddressRepository addresses;
  private final UserRepository users;
  private final PasswordEncoder encoder;

  public AccountApiController(
      CurrentUser c, AddressRepository a, UserRepository u, PasswordEncoder e) {
    current = c;
    addresses = a;
    users = u;
    encoder = e;
  }

  @GetMapping("/addresses")
  List<Address> addresses() {
    return addresses.findByUserIdOrderByDefaultAddressDesc(current.required().getId());
  }

  @PostMapping("/addresses")
  Address create(@RequestBody Address address) {
    User u = current.required();
    address.setId(null);
    address.setUser(u);
    if (address.isDefaultAddress())
      addresses
          .findByUserIdOrderByDefaultAddressDesc(u.getId())
          .forEach(a -> a.setDefaultAddress(false));
    return addresses.save(address);
  }

  @PostMapping("/change-password")
  Map<String, String> password(@RequestBody Map<String, String> body) {
    User u = current.required();
    if (!encoder.matches(body.get("oldPassword"), u.getPassword()))
      throw new IllegalArgumentException("Mật khẩu cũ không đúng");
    String next = body.get("newPassword");
    if (next == null || next.length() < 8)
      throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự");
    u.setPassword(encoder.encode(next));
    users.save(u);
    return Map.of("message", "Đã đổi mật khẩu");
  }
}
