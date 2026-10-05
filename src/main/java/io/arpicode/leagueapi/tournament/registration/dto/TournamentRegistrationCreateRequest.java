package io.arpicode.leagueapi.tournament.registration.dto;

import jakarta.validation.constraints.NotNull;

public record TournamentRegistrationCreateRequest(

    @NotNull(message = "Player must be set")
    Long playerId

) {
}
