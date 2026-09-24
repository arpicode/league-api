package io.arpicode.leagueapi.shared.error;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMessages {

    public static final String PLAYER_NOT_FOUND = "Player of ID %d not found";
    public static final String USERNAME_ALREADY_EXISTS = "Username already exists";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";

    public static final String BOARD_GAME_NOT_FOUND = "Board game of ID %d not found";
    public static final String BOARD_GAME_NAME_ALREADY_EXISTS = "Board game name already exists";
    public static final String BOARD_GAME_IN_USE = "Board game is used by a tournament and can't be deleted";

    public static final String TOURNAMENT_NOT_FOUND = "Tournament of ID %d not found";
    public static final String TOURNAMENT_NAME_ALREADY_EXISTS = "Tournament name already exists";
    public static final String TOURNAMENT_ILLEGAL_TRANSITION = "Tournament status can't transition from %s to %s";
    public static final String TOURNAMENT_BOARD_GAME_LOCKED = "Tournament board game can't be changed once status is %s";
    public static final String TOURNAMENT_LOCKED = "Tournament can't be modified once status is %s";
    public static final String TOURNAMENT_NOT_DELETABLE = "Tournament can't be deleted once status is %s, cancel it instead";

    public static final String CONFLICT = "The request conflicts with an existing resource.";
    public static final String CONCURRENT_MODIFICATION = "The resource was modified by another request. Retry with its current state.";
    public static final String VALIDATION_ERROR = "Request validation failed. See 'errors' for details.";
    public static final String INTERNAL_ERROR = "An unexpected error occurred. Quote the errorId when reporting it.";

}
