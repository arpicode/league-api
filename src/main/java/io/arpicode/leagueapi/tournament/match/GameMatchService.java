package io.arpicode.leagueapi.tournament.match;

import io.arpicode.leagueapi.player.Player;
import io.arpicode.leagueapi.player.dto.PlayerSummary;
import io.arpicode.leagueapi.shared.clock.LeagueTime;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.Tournament;
import io.arpicode.leagueapi.tournament.TournamentRepository;
import io.arpicode.leagueapi.tournament.match.dto.GameMatchCreateRequest;
import io.arpicode.leagueapi.tournament.match.dto.GameMatchResponse;
import io.arpicode.leagueapi.tournament.match.dto.MatchParticipantResponse;
import io.arpicode.leagueapi.tournament.registration.TournamentRegistration;
import io.arpicode.leagueapi.tournament.registration.TournamentRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameMatchService {

    private final GameMatchRepository gameMatchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;

    public GameMatchService(
        GameMatchRepository gameMatchRepository,
        TournamentRepository tournamentRepository,
        TournamentRegistrationRepository tournamentRegistrationRepository
    ) {
        this.gameMatchRepository = gameMatchRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentRegistrationRepository = tournamentRegistrationRepository;
    }

    // The tournament lock keeps every check below true until the insert: the status, the dates and
    // the roster can only change through requests that take the same lock.
    @Transactional
    public GameMatchResponse create(long tournamentId, GameMatchCreateRequest gameMatchCreateRequest) {
        Tournament tournament = findTournamentForUpdate(tournamentId);

        // First, so a tournament that isn't running says so, rather than failing on a consequence
        // of it: a draft has no roster, so its players would be reported as unregistered.
        tournament.assertInProgress();

        // No lower bound from the clock: organizers enter matches after they are played.
        OffsetDateTime scheduledAt = gameMatchCreateRequest.scheduledAt();
        if (scheduledAt != null) {
            tournament.assertMatchDayWithinDates(leagueDay(scheduledAt));
        }

        List<Player> players = findRegisteredPlayers(tournament.getId(), gameMatchCreateRequest.playerIds());

        GameMatch gameMatch = new GameMatch(
            tournament,
            gameMatchCreateRequest.roundNumber(),
            scheduledAt,
            players
        );
        GameMatch saved = gameMatchRepository.saveAndFlush(gameMatch);

        return toGameMatchResponse(saved);
    }

    @Transactional(readOnly = true)
    public GameMatchResponse getById(long tournamentId, long id) {
        GameMatch gameMatch = gameMatchRepository.findByIdAndTournamentId(id, tournamentId)
            .orElseThrow(() -> gameMatchNotFound(tournamentId, id));

        return toGameMatchResponse(gameMatch);
    }

    // For a tournament whose dates are changing. The caller must hold the tournament lock, as
    // TournamentService.update does, or a match could be scheduled outside the new dates between
    // this read and the commit.
    public void assertMatchesWithinDates(Tournament tournament) {
        GameMatchRepository.ScheduleRange range = gameMatchRepository.findScheduleRange(tournament.getId());

        if (range.getEarliest() == null) {
            return;
        }

        tournament.assertDatesCoverMatchDays(leagueDay(range.getEarliest()), leagueDay(range.getLatest()));
    }

    // Tournament dates are calendar days in the league's zone, so a match's instant is converted to
    // that zone before it is compared with them: 23:30 UTC on the 10th is already the 11th in Paris.
    private LocalDate leagueDay(OffsetDateTime instant) {
        return instant.atZoneSameInstant(LeagueTime.ZONE).toLocalDate();
    }

    // In the request's order, which is the seat order. Every registration left from IN_PROGRESS is
    // confirmed (V009), so being registered is enough. Checked here rather than left to V013's
    // foreign key, which would only report a generic violation without naming the players.
    private List<Player> findRegisteredPlayers(long tournamentId, List<Long> playerIds) {
        Map<Long, Player> registered = tournamentRegistrationRepository
            .findByTournamentIdAndPlayerIdIn(tournamentId, playerIds).stream()
            .collect(Collectors.toMap(
                registration -> registration.getId().getPlayerId(),
                TournamentRegistration::getPlayer
            ));

        List<Long> unregistered = playerIds.stream()
            .filter(playerId -> !registered.containsKey(playerId))
            .toList();

        if (!unregistered.isEmpty()) {
            throw new BusinessException(
                ErrorCode.GAME_MATCH_PLAYERS_NOT_REGISTERED,
                UserMessages.GAME_MATCH_PLAYERS_NOT_REGISTERED.formatted(unregistered, tournamentId)
            );
        }

        return playerIds.stream().map(registered::get).toList();
    }

    // The tournament is the parent resource in the URL, so a missing one is reported as such rather
    // than as a missing match, as for registrations. It is only looked up on this path.
    private BusinessException gameMatchNotFound(long tournamentId, long id) {
        if (!tournamentRepository.existsById(tournamentId)) {
            return new BusinessException(
                ErrorCode.TOURNAMENT_NOT_FOUND,
                UserMessages.TOURNAMENT_NOT_FOUND.formatted(tournamentId)
            );
        }
        return new BusinessException(
            ErrorCode.GAME_MATCH_NOT_FOUND,
            UserMessages.GAME_MATCH_NOT_FOUND.formatted(id, tournamentId)
        );
    }

    private GameMatchResponse toGameMatchResponse(GameMatch gameMatch) {
        return new GameMatchResponse(
            gameMatch.getId(),
            gameMatch.getTournament().getId(),
            gameMatch.getStatus(),
            gameMatch.getRoundNumber(),
            gameMatch.getScheduledAt(),
            gameMatch.getStartedAt(),
            gameMatch.getCompletedAt(),
            gameMatch.getParticipants().stream()
                .map(participant -> new MatchParticipantResponse(
                    participant.getSeatNumber(),
                    new PlayerSummary(participant.getPlayer().getId(), participant.getPlayer().getUsername())
                ))
                .toList(),
            gameMatch.getCreatedAt(),
            gameMatch.getUpdatedAt()
        );
    }

    private Tournament findTournamentForUpdate(long id) {
        return tournamentRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.TOURNAMENT_NOT_FOUND,
                UserMessages.TOURNAMENT_NOT_FOUND.formatted(id)
            ));
    }
}
