package io.arpicode.leagueapi.boardgame.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Asserts that a board game's player bounds are consistent, i.e. {@code maxPlayers >= minPlayers}.
 * <p>
 * Scoped to this package on purpose: the validator is typed to {@link BoardGameRequest}, so the
 * constraint is board-game specific despite the general-sounding name. Widen it (via an interface)
 * only when a second type actually needs the rule.
 */
@Documented
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PlayerRangeValidator.class)
public @interface PlayerRange {

    String message() default "Maximum players must be greater than or equal to minimum players";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
