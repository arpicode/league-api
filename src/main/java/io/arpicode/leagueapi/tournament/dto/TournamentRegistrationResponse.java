package io.arpicode.leagueapi.tournament.dto;

import io.arpicode.leagueapi.player.dto.PlayerSummary;
import io.arpicode.leagueapi.tournament.TournamentRegistrationStatus;

import java.time.OffsetDateTime;

/**
 * A registration as a tournament's roster lists it.
 *
 * @param waitlistPosition 1-based place on the waitlist, 1 being the next registration to be
 *                         promoted; {@code null} when the registration is {@code CONFIRMED}
 */
public record TournamentRegistrationResponse(
        Long tournamentId,
        PlayerSummary player,
        OffsetDateTime registeredAt,
        TournamentRegistrationStatus status,
        Integer waitlistPosition) {
}
