package com.liveticket.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 32, message = "username length must be 3-32")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 64, message = "password length must be 6-64")
    private String password;

    @NotBlank(message = "nickname is required")
    @Size(max = 32, message = "nickname length must be <= 32")
    private String nickname;
}
