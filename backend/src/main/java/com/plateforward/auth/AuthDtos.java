package com.plateforward.auth;

import com.plateforward.user.User;
import com.plateforward.user.UserDto;
import jakarta.validation.constraints.*;

public interface AuthDtos {
    record RegisterRequest(@Email @NotBlank String email, @Size(min = 8, message = "must be at least 8 characters") String password,
                           @NotBlank String name, @NotNull User.Role role, String phone, @NotBlank String area) {}

    record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    record AuthResponse(String token, UserDto user) {}
}
