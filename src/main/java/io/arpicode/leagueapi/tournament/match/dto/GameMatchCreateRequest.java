package io.arpicode.leagueapi.tournament.match.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UniqueElements;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @param scheduledAt optional, since a match can be created before it has a time. ISO 8601 with an
 *                    offset, so the instant is the client's to state rather than the server's to guess
 * @param playerIds   the players in seat order, the first one taking seat 1
 */
public record GameMatchCreateRequest(

    @Positive(message = "Round number must be at least 1")
    Short roundNumber,

    OffsetDateTime scheduledAt,

    @NotEmpty(message = "Players must be set")
    @UniqueElements(message = "A player can only take one seat")
    List<@NotNull(message = "Player must be set") Long> playerIds
) {
}
