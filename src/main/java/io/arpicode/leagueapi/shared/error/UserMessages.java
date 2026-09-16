package io.arpicode.leagueapi.shared.error;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMessages {

    public static final String PLAYER_NOT_FOUND = "Player of ID %d not found";
    public static final String USERNAME_ALREADY_EXISTS = "Username already exists";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    public static final String CONFLICT = "The request conflicts with an existing resource.";
    public static final String VALIDATION_ERROR = "Request validation failed. See 'errors' for details.";

}
