package io.arpicode.leagueapi.tournament.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TournamentDatesValidator implements ConstraintValidator<TournamentDates, TournamentSchedule> {

    @Override
    public boolean isValid(TournamentSchedule request, ConstraintValidatorContext context) {
        if (request == null || request.endsOn() == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        // Mirrors ck_tournament_dates clause A: an end date is meaningless on its own.
        if (request.startsOn() == null) {
            context.buildConstraintViolationWithTemplate("A start date is required when an end date is set")
                    .addPropertyNode("startsOn")
                    .addConstraintViolation();

            return false;
        }

        // Clause B.
        if (request.endsOn().isBefore(request.startsOn())) {
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("endsOn")
                    .addConstraintViolation();

            return false;
        }

        return true;
    }

}
