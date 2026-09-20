package com.utepetshop.dto;

import jakarta.validation.constraints.*;

public record AuthRequest(
    @Email @NotBlank String email, @NotBlank @Size(min = 8) String password) {}
