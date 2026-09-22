package io.arpicode.leagueapi.tournament.dto;

import java.time.LocalDate;

public interface TournamentSchedule {
    LocalDate startsOn();

    LocalDate endsOn();

}
