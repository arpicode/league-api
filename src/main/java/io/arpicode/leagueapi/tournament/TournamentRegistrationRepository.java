package io.arpicode.leagueapi.tournament;

import lombok.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, TournamentPlayerId> {

    // Every response embeds a PlayerSummary and open-in-view is off, so the player is fetched up front.
    @Override
    @NonNull
    @EntityGraph(attributePaths = "player")
    Optional<TournamentRegistration> findById(@NonNull TournamentPlayerId id);

    long countByStatusAndTournamentId(TournamentRegistrationStatus status, Long tournamentId);

    // A waitlisted registration's position is its rank in the order V009 defines: registered_at,
    // then player_id to break ties. Empty when the player is not on the tournament's waitlist.
    @Query(value = """
            SELECT ranked.position
            FROM (SELECT player_id,
                         ROW_NUMBER() OVER (ORDER BY registered_at, player_id) AS position
                    FROM league.tournament_registration
                   WHERE tournament_id = :tournamentId
                     AND status = 'WAITLISTED') ranked
            WHERE ranked.player_id = :playerId
            """, nativeQuery = true)
    Optional<Long> findWaitlistPosition(@Param("tournamentId") Long tournamentId,
                                        @Param("playerId") Long playerId);
}
