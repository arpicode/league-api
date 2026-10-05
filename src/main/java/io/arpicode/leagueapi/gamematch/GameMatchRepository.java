package io.arpicode.leagueapi.gamematch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {

    // The first and last schedule among a tournament's matches, both null when none is scheduled.
    // Cancelled matches no longer take place on their date, so they don't hold the tournament's dates.
    @Query("""
        SELECT min(gameMatch.scheduledAt) AS earliest, max(gameMatch.scheduledAt) AS latest
        FROM GameMatch gameMatch
        WHERE gameMatch.tournament.id = :tournamentId
          AND gameMatch.status <> io.arpicode.leagueapi.gamematch.GameMatchStatus.CANCELLED
        """)
    ScheduleRange findScheduleRange(@Param("tournamentId") Long tournamentId);

    interface ScheduleRange {
        OffsetDateTime getEarliest();

        OffsetDateTime getLatest();
    }
}
