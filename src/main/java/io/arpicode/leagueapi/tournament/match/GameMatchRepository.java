package io.arpicode.leagueapi.tournament.match;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {

    // Scoped to the tournament in the URL, so a match id from another tournament is not found here.
    // Every response lists the participants with a PlayerSummary each, hence the graph.
    @EntityGraph(attributePaths = {"participants", "participants.player"})
    Optional<GameMatch> findByIdAndTournamentId(Long id, Long tournamentId);

    // The first and last schedule among a tournament's matches, both null when none is scheduled.
    // Cancelled matches no longer take place on their date, so they don't hold the tournament's dates.
    @Query("""
        SELECT min(gameMatch.scheduledAt) AS earliest, max(gameMatch.scheduledAt) AS latest
        FROM GameMatch gameMatch
        WHERE gameMatch.tournament.id = :tournamentId
          AND gameMatch.status <> io.arpicode.leagueapi.tournament.match.GameMatchStatus.CANCELLED
        """)
    ScheduleRange findScheduleRange(@Param("tournamentId") Long tournamentId);

    interface ScheduleRange {
        OffsetDateTime getEarliest();

        OffsetDateTime getLatest();
    }
}
