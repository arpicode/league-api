package io.arpicode.leagueapi.tournament;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, TournamentPlayerId> {

    long countByStatusAndTournamentId(TournamentRegistrationStatus status, Long tournamentId);
}
