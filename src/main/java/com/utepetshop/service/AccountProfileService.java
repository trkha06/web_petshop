package com.utepetshop.service;

import com.utepetshop.entity.Address;
import com.utepetshop.entity.User;
import com.utepetshop.repository.AddressRepository;
import com.utepetshop.repository.UserRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProfileService {
  private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9][0-9 .()-]{8,19}$");

  private final UserRepository users;
  private final AddressRepository addresses;
  private final PasswordEncoder passwordEncoder;

  public AccountProfileService(
      UserRepository users, AddressRepository addresses, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.addresses = addresses;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public User updateProfile(User user, String fullName, String phone) {
    String normalizedName = normalize(fullName);
    String normalizedPhone = normalize(phone);

    if (normalizedName == null || normalizedName.length() < 2 || normalizedName.length() > 100)
      throw new IllegalArgumentException("Họ tên phải có từ 2 đến 100 ký tự");

    validatePhone(normalizedPhone, false);

    user.setFullName(normalizedName);
    user.setPhone(normalizedPhone);
    return users.save(user);
  }

  @Transactional
  public void changePassword(
      User user, String currentPassword, String newPassword, String confirmPassword) {
    if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword()))
      throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");

    if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72)
      throw new IllegalArgumentException("Mật khẩu mới phải có từ 8 đến 72 ký tự");

    if (!newPassword.equals(confirmPassword))
      throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp");

    if (passwordEncoder.matches(newPassword, user.getPassword()))
      throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");

    user.setPassword(passwordEncoder.encode(newPassword));
    users.save(user);
  }

  @Transactional
  public Address addAddress(
      User user,
      String recipientName,
      String phone,
      String line1,
      String ward,
      String district,
      String city,
      boolean defaultAddress) {
    String normalizedName = require(recipientName, "Vui lòng nhập tên người nhận", 100);
    String normalizedPhone = require(phone, "Vui lòng nhập số điện thoại", 20);
    String normalizedLine1 = require(line1, "Vui lòng nhập số nhà và tên đường", 255);
    String normalizedCity = require(city, "Vui lòng nhập tỉnh hoặc thành phố", 100);
    validatePhone(normalizedPhone, true);

    List<Address> existing = addresses.findByUserIdOrderByDefaultAddressDesc(user.getId());
    boolean makeDefault = defaultAddress || existing.isEmpty();
    if (makeDefault) {
      existing.forEach(address -> address.setDefaultAddress(false));
      addresses.saveAll(existing);
    }

    Address address = new Address();
    address.setUser(user);
    address.setRecipientName(normalizedName);
    address.setPhone(normalizedPhone);
    address.setLine1(normalizedLine1);
    address.setWard(limit(ward, 100, "Phường hoặc xã quá dài"));
    address.setDistrict(limit(district, 100, "Quận hoặc huyện quá dài"));
    address.setCity(normalizedCity);
    address.setDefaultAddress(makeDefault);
    return addresses.save(address);
  }

  @Transactional
  public void setDefaultAddress(User user, Long addressId) {
    Address selected = ownedAddress(user, addressId);
    List<Address> existing = addresses.findByUserIdOrderByDefaultAddressDesc(user.getId());
    existing.forEach(address -> address.setDefaultAddress(address.getId().equals(selected.getId())));
    addresses.saveAll(existing);
  }

  @Transactional
  public void deleteAddress(User user, Long addressId) {
    Address selected = ownedAddress(user, addressId);
    boolean wasDefault = selected.isDefaultAddress();
    addresses.delete(selected);
    addresses.flush();

    if (wasDefault) {
      addresses.findByUserIdOrderByDefaultAddressDesc(user.getId()).stream()
          .findFirst()
          .ifPresent(
              address -> {
                address.setDefaultAddress(true);
                addresses.save(address);
              });
    }
  }

  private Address ownedAddress(User user, Long addressId) {
    return addresses
        .findByIdAndUserId(addressId, user.getId())
        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ của bạn"));
  }

  private void validatePhone(String phone, boolean required) {
    if (phone == null) {
      if (required) throw new IllegalArgumentException("Vui lòng nhập số điện thoại");
      return;
    }

    String digits = phone.replaceAll("\\D", "");
    if (!PHONE_PATTERN.matcher(phone).matches() || digits.length() < 9 || digits.length() > 12)
      throw new IllegalArgumentException("Số điện thoại không hợp lệ");
  }

  private String require(String value, String message, int maxLength) {
    String normalized = normalize(value);
    if (normalized == null) throw new IllegalArgumentException(message);
    if (normalized.length() > maxLength) throw new IllegalArgumentException(message);
    return normalized;
  }

  private String limit(String value, int maxLength, String message) {
    String normalized = normalize(value);
    if (normalized != null && normalized.length() > maxLength)
      throw new IllegalArgumentException(message);
    return normalized;
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
