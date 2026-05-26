package com.istrazidomace.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username je obavezan")
    @Size(min = 3, max = 50, message = "Username mora biti između 3 i 50 karaktera")
    private String username;

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email nije validan")
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 6, message = "Lozinka mora imati najmanje 6 karaktera")
    private String password;
}