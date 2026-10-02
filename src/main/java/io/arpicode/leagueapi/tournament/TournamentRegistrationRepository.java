package io.arpicode.leagueapi.tournament;

import lombok.NonNull;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, TournamentPlayerId> {

    // Every response embeds a PlayerSummary and open-in-view is off, so the player is fetched up front.
    @Override
    @NonNull
    @EntityGraph(attributePaths = "player")
    Optional<TournamentRegistration> findById(@NonNull TournamentPlayerId id);

    // The roster embeds a PlayerSummary per row, so the players come with the page.
    @EntityGraph(attributePaths = "player")
    Page<TournamentRegistration> findByTournamentId(Long tournamentId, Pageable pageable);

    long countByStatusAndTournamentId(TournamentRegistrationStatus status, Long tournamentId);

    // The head of the waitlist, in the same order findWaitlistPosition ranks it: registered_at, then
    // player_id to break ties. The limit is the number of places being filled.
    List<TournamentRegistration> findByTournamentIdAndStatusOrderByRegisteredAtAscIdPlayerIdAsc(
        Long tournamentId, TournamentRegistrationStatus status, Limit limit);

    List<TournamentRegistration> findByTournamentIdAndStatus(Long tournamentId, TournamentRegistrationStatus status);

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

    // The same rank for several players at once, so a roster page costs one query rather than one
    // per waitlisted row. Players not on the waitlist are simply absent from the result. The aliases
    // are quoted because Postgres folds unquoted ones to lowercase, which WaitlistPosition's getters
    // would not match.
    @Query(value = """
        SELECT ranked.player_id AS "playerId", ranked.position AS "position"
        FROM (SELECT player_id,
                     ROW_NUMBER() OVER (ORDER BY registered_at, player_id) AS position
                FROM league.tournament_registration
               WHERE tournament_id = :tournamentId
                 AND status = 'WAITLISTED') ranked
        WHERE ranked.player_id IN (:playerIds)
        """, nativeQuery = true)
    List<WaitlistPosition> findWaitlistPositions(@Param("tournamentId") Long tournamentId,
                                                 @Param("playerIds") Collection<Long> playerIds);

    interface WaitlistPosition {
        Long getPlayerId();

        Long getPosition();
    }
}
