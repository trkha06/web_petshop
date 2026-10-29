package com.utepetshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @NotBlank @Size(max = 100) String recipientName,
    @NotBlank @Size(max = 20) String phone,
    @NotBlank @Size(max = 255) String line1,
    @Size(max = 100) String ward,
    @Size(max = 100) String district,
    @Size(max = 100) String city,
    boolean defaultAddress) {}
