package io.arpicode.leagueapi.tournament.dto;

import jakarta.validation.constraints.NotNull;

public record TournamentRegistrationCreateRequest(

        @NotNull(message = "Player must be set")
        Long playerId

) {
}
