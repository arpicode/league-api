package io.arpicode.leagueapi.player.dto;

/**
 * The player as other features embed it: enough to label them.
 * <p>
 * Deliberately not {@link PlayerResponse}: embedded views such as a tournament's roster are visible
 * to anyone looking at the tournament, so the email stays out.
 */
public record PlayerSummary(
    Long id,
    String username
) {
}
