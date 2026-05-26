package com.istrazidomace.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email nije validan")
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    private String password;
}