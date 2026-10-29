package com.utepetshop.dto;

import com.utepetshop.entity.Address;
import java.time.LocalDateTime;

public record AddressResponse(
    Long id,
    String recipientName,
    String phone,
    String line1,
    String ward,
    String district,
    String city,
    boolean defaultAddress,
    LocalDateTime createdAt) {
  public static AddressResponse fromEntity(Address address) {
    return new AddressResponse(
        address.getId(),
        address.getRecipientName(),
        address.getPhone(),
        address.getLine1(),
        address.getWard(),
        address.getDistrict(),
        address.getCity(),
        address.isDefaultAddress(),
        address.getCreatedAt());
  }
}
