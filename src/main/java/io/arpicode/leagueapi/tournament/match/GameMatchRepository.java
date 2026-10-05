package io.arpicode.leagueapi.tournament.match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {

    // Scoped to the tournament in the URL, so a match id from another tournament is not found here.
    // Every response lists the participants with a PlayerSummary each, hence the graph.
    @EntityGraph(attributePaths = {"participants", "participants.player"})
    Optional<GameMatch> findByIdAndTournamentId(Long id, Long tournamentId);

    // Without the participants: fetching a collection in a paged query makes Hibernate page in memory,
    // so findParticipants loads them for the whole page instead.
    Page<GameMatch> findByTournamentId(Long tournamentId, Pageable pageable);

    // The participants of a page of matches in one query rather than one per match, with the player
    // each response names, in seat order within each match.
    @Query("""
        SELECT participant
        FROM MatchParticipant participant
        JOIN FETCH participant.player
        WHERE participant.gameMatch.id IN :gameMatchIds
        ORDER BY participant.seatNumber
        """)
    List<MatchParticipant> findParticipants(@Param("gameMatchIds") Collection<Long> gameMatchIds);

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
