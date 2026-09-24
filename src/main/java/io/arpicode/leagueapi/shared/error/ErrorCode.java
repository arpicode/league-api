package io.arpicode.leagueapi.shared.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Application-level error vocabulary, deliberately free of any transport type.
 * <p>
 * Each code carries a {@link Kind} rather than an HTTP status: the mapping to HTTP lives in
 * {@code GlobalExceptionHandler}, so domain code (an entity rejecting an illegal state change,
 * a service reporting a missing row) can raise a {@link BusinessException} without importing
 * anything web-related. Adding a code is a one-line change here, and the constructor makes
 * forgetting its kind a compile error.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Business rules violations
    PLAYER_NOT_FOUND(Kind.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(Kind.CONFLICT),
    EMAIL_ALREADY_EXISTS(Kind.CONFLICT),

    BOARD_GAME_NOT_FOUND(Kind.NOT_FOUND),
    BOARD_GAME_NAME_ALREADY_EXISTS(Kind.CONFLICT),
    BOARD_GAME_IN_USE(Kind.CONFLICT),

    TOURNAMENT_NOT_FOUND(Kind.NOT_FOUND),
    TOURNAMENT_NAME_ALREADY_EXISTS(Kind.CONFLICT),
    TOURNAMENT_ILLEGAL_TRANSITION(Kind.CONFLICT),
    TOURNAMENT_BOARD_GAME_LOCKED(Kind.CONFLICT),
    TOURNAMENT_LOCKED(Kind.CONFLICT),

    DATA_INTEGRITY_VIOLATION(Kind.CONFLICT),
    VALIDATION_ERROR(Kind.INVALID_REQUEST),

    // Protocol-level failures raised by Spring before or around the controller
    MALFORMED_REQUEST(Kind.INVALID_REQUEST),
    NOT_FOUND(Kind.NOT_FOUND),
    METHOD_NOT_ALLOWED(Kind.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(Kind.UNSUPPORTED_MEDIA_TYPE),

    // Anything that reaches the handler unrecognised
    INTERNAL_ERROR(Kind.INTERNAL),
    ;

    private final Kind kind;

    /**
     * Transport-neutral category a code belongs to. Kinds are few and stable, so the switch
     * that turns one into an HTTP status does not grow as features add codes.
     */
    public enum Kind {
        NOT_FOUND,
        CONFLICT,
        INVALID_REQUEST,
        METHOD_NOT_ALLOWED,
        UNSUPPORTED_MEDIA_TYPE,
        INTERNAL,
    }
}
