package io.arpicode.leagueapi.boardgame.dto;

import java.time.OffsetDateTime;

public record BoardGameResponse(
        Long id,
        String name,
        Short minPlayers,
        Short maxPlayers,
        Short avgDurationMin,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
