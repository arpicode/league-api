package io.arpicode.leagueapi.player.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String email
) {

    public PlayerRequest {
        username = username == null ? null : username.strip();
        email = email == null ? null : email.strip();
    }

}
