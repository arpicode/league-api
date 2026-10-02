package io.arpicode.leagueapi.tournament.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TournamentDatesValidator.class)
public @interface TournamentDates {

    String message() default "End date can't be before the start date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
