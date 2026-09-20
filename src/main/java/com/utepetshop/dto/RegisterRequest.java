package com.utepetshop.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank String fullName,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password) {}
