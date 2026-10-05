package io.arpicode.leagueapi.gamematch.dto;

import io.arpicode.leagueapi.gamematch.GameMatchStatus;
import io.arpicode.leagueapi.tournament.dto.TournamentSummary;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @param participants in seat order
 */
public record GameMatchResponse(
    Long id,
    TournamentSummary tournament,
    GameMatchStatus status,
    Short roundNumber,
    OffsetDateTime scheduledAt,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    List<MatchParticipantResponse> participants,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
}
