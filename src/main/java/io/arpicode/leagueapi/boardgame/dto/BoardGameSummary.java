package io.arpicode.leagueapi.boardgame.dto;

/**
 * The board game as other features embed it: enough to label it.
 * <p>
 * Deliberately not {@link BoardGameResponse}: a tournament's client wants a name to display.
 */
public record BoardGameSummary(Long id, String name) {
}
