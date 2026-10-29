package com.utepetshop.controller.api;

import com.utepetshop.dto.AddressRequest;
import com.utepetshop.dto.AddressResponse;
import com.utepetshop.entity.Address;
import com.utepetshop.entity.User;
import com.utepetshop.repository.*;
import com.utepetshop.service.CurrentUser;
import jakarta.validation.Valid;
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
  List<AddressResponse> addresses() {
    return addresses.findByUserIdOrderByDefaultAddressDesc(current.required().getId()).stream()
        .map(AddressResponse::fromEntity)
        .toList();
  }

  @PostMapping("/addresses")
  AddressResponse create(@Valid @RequestBody AddressRequest request) {
    User u = current.required();
    Address address = new Address();
    address.setUser(u);
    address.setRecipientName(request.recipientName().trim());
    address.setPhone(request.phone().trim());
    address.setLine1(request.line1().trim());
    address.setWard(normalize(request.ward()));
    address.setDistrict(normalize(request.district()));
    address.setCity(normalize(request.city()));
    address.setDefaultAddress(request.defaultAddress());
    if (request.defaultAddress()) {
      List<Address> existing = addresses.findByUserIdOrderByDefaultAddressDesc(u.getId());
      existing.forEach(item -> item.setDefaultAddress(false));
      addresses.saveAll(existing);
    }
    return AddressResponse.fromEntity(addresses.save(address));
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

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
