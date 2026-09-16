package io.arpicode.leagueapi.shared.error;

public enum ErrorCode {
    // Business rules violations
    PLAYER_NOT_FOUND,
    USERNAME_ALREADY_EXISTS,
    EMAIL_ALREADY_EXISTS,
    DATA_INTEGRITY_VIOLATION,
    VALIDATION_ERROR,

    // Protocol-level failures raised by Spring before or around the controller
    MALFORMED_REQUEST,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,

    // Anything that reaches the handler unrecognised
    INTERNAL_ERROR,
}
