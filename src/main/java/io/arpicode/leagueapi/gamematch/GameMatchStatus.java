package io.arpicode.leagueapi.gamematch;

public enum GameMatchStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    // Unlike TournamentStatus, no status transitions to itself: each transition is an action
    // (start, complete, cancel), and repeating one is a conflict, not an idempotent replay.
    public boolean canTransitionTo(GameMatchStatus toStatus) {
        return switch (this) {
            case SCHEDULED -> toStatus == IN_PROGRESS || toStatus == CANCELLED;
            case IN_PROGRESS -> toStatus == COMPLETED || toStatus == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
