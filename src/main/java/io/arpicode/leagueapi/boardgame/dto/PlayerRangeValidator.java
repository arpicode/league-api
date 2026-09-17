package io.arpicode.leagueapi.boardgame.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlayerRangeValidator implements ConstraintValidator<PlayerRange, BoardGameRequest> {

    @Override
    public boolean isValid(BoardGameRequest request, ConstraintValidatorContext context) {
        if (request == null || request.minPlayers() == null || request.maxPlayers() == null) {
            return true; // incomplete input is @NotNull's problem, not this rule's
        }
        if (request.maxPlayers() >= request.minPlayers()) {
            return true;
        }

        // Report against maxPlayers rather than the record itself: a class-level violation
        // becomes a global error, and GlobalExceptionHandler only reads getFieldErrors(),
        // so it would reach the client as a 400 with an empty "errors" array.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("maxPlayers")
                .addConstraintViolation();

        return false;
    }

}
