package io.arpicode.leagueapi.tournament.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@TournamentDates
public record TournamentCreateRequest(

        @NotNull(message = "Board game must be set")
        Long boardGameId,

        @NotBlank(message = "Name cannot be blank")
        @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
        String name,

        @Min(value = 2, message = "Maximum players must be at least 2")
        Short maxPlayers,

        LocalDate startsOn,
        LocalDate endsOn

) implements TournamentSchedule {

    public TournamentCreateRequest {
        name = name == null ? null : name.strip();
    }

}
