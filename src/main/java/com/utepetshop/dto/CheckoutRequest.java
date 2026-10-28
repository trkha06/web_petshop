package com.utepetshop.dto;

import com.utepetshop.entity.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
    @NotNull PaymentMethod paymentMethod,
    @NotNull DeliveryType deliveryType,
    @NotBlank @Size(max = 100) String recipientName,
    @NotBlank @Email @Size(max = 150) String recipientEmail,
    @NotBlank @Size(max = 20) String recipientPhone,
    @Size(max = 500) String shippingAddress,
    @Size(max = 500) String customerNote,
    @Size(max = 50) String promotionCode) {}
