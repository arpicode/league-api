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
    public static final String TOURNAMENT_MAX_PLAYERS_BELOW_CONFIRMED = "Maximum players can't be lower than the %d players already confirmed";

    public static final String TOURNAMENT_NOT_OPEN = "Tournament status must be OPEN to register. Current status is %s";
    public static final String TOURNAMENT_NOT_OPEN_FOR_WITHDRAWAL = "Tournament status must be OPEN to withdraw. Current status is %s";
    public static final String TOURNAMENT_NOT_IN_PROGRESS_FOR_GAME_MATCH = "Tournament status must be IN_PROGRESS to create a game match. Current status is %s";
    public static final String TOURNAMENT_REGISTRATION_PLAYER_ALREADY_REGISTERED = "Player of id %d is already registered for tournament of id %d";
    public static final String TOURNAMENT_REGISTRATION_ALREADY_EXISTS = "Tournament registration already exists";
    public static final String TOURNAMENT_REGISTRATION_NOT_FOUND = "Player of id %d is not registered for tournament of id %d";

    public static final String GAME_MATCH_ILLEGAL_TRANSITION = "Game match status can't transition from %s to %s";

    public static final String CONFLICT = "The request conflicts with an existing resource.";
    public static final String CONCURRENT_MODIFICATION = "The resource was modified by another request. Retry with its current state.";
    public static final String VALIDATION_ERROR = "Request validation failed. See 'errors' for details.";
    public static final String INTERNAL_ERROR = "An unexpected error occurred. Quote the errorId when reporting it.";

}
