package com.utepetshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegistrationOtpRequest(
    @Email @NotBlank String email,
    @NotBlank @Pattern(regexp = "\\d{6}") String otp) {}
