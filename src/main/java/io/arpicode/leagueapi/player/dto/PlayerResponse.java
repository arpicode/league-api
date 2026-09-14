package io.arpicode.leagueapi.player.dto;

import java.time.OffsetDateTime;

public record PlayerResponse(
        Long id,
        String username,
        String email,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
