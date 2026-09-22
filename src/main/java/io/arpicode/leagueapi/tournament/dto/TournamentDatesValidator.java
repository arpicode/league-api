package io.arpicode.leagueapi.tournament.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TournamentDatesValidator implements ConstraintValidator<TournamentDates, TournamentRequest> {

    @Override
    public boolean isValid(TournamentRequest request, ConstraintValidatorContext context) {
        if (request == null ||
                request.endsOn() == null ||
                request.startsOn() == null ||
                request.endsOn().isEqual(request.startsOn()) ||
                request.endsOn().isAfter(request.startsOn())
        ) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endsOn")
                .addConstraintViolation();

        return false;
    }

}
