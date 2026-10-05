package io.arpicode.leagueapi.gamematch.dto;

import io.arpicode.leagueapi.player.dto.PlayerSummary;

// Score and outcome join it with result entry.
public record MatchParticipantResponse(
    Short seatNumber,
    PlayerSummary player) {
}
