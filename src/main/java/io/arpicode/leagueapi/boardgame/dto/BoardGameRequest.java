package io.arpicode.leagueapi.boardgame.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@PlayerRange
public record BoardGameRequest(
        @NotBlank(message = "Name cannot be blank")
        @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
        String name,

        @NotNull(message = "Minimum players must be set")
        @Min(value = 1, message = "Minimum players must be at least 1")
        Short minPlayers,

        @NotNull(message = "Maximum players must be set")
        Short maxPlayers,

        @Min(value = 1, message = "Average duration (minutes) must be at least 1")
        Short avgDurationMin
) {

    public BoardGameRequest {
        name = name == null ? null : name.strip();
    }

}
