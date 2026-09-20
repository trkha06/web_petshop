package com.utepetshop.dto;

import com.utepetshop.entity.*;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
    @NotNull PaymentMethod paymentMethod, @NotNull DeliveryType deliveryType) {}
