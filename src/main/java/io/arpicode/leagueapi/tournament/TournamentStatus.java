package io.arpicode.leagueapi.tournament;

public enum TournamentStatus {
    DRAFT,
    OPEN,
    IN_PROGRESS,
    CLOSED,
    CANCELLED;

    public boolean canTransitionTo(TournamentStatus toStatus) {
        return switch (this) {
            case DRAFT -> toStatus == DRAFT || toStatus == OPEN || toStatus == CANCELLED;
            case OPEN -> toStatus == OPEN || toStatus == IN_PROGRESS || toStatus == CANCELLED;
            case IN_PROGRESS -> toStatus == IN_PROGRESS || toStatus == CLOSED;
            case CLOSED -> toStatus == CLOSED;
            case CANCELLED -> toStatus == CANCELLED;
        };
    }
}
