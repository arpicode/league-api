package io.arpicode.leagueapi.tournament.dto;

import io.arpicode.leagueapi.boardgame.dto.BoardGameSummary;
import io.arpicode.leagueapi.tournament.TournamentStatus;

import java.time.LocalDate;

public record TournamentSummary(
    Long id,
    BoardGameSummary boardGame,
    String name,
    TournamentStatus status,
    Short maxPlayers,
    LocalDate startsOn,
    LocalDate endsOn) {
}
