package io.arpicode.leagueapi.gamematch;

import io.arpicode.leagueapi.boardgame.BoardGame;
import io.arpicode.leagueapi.boardgame.dto.BoardGameSummary;
import io.arpicode.leagueapi.gamematch.dto.GameMatchCreateRequest;
import io.arpicode.leagueapi.gamematch.dto.GameMatchResponse;
import io.arpicode.leagueapi.gamematch.dto.MatchParticipantResponse;
import io.arpicode.leagueapi.player.Player;
import io.arpicode.leagueapi.player.dto.PlayerSummary;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.Tournament;
import io.arpicode.leagueapi.tournament.TournamentRegistration;
import io.arpicode.leagueapi.tournament.TournamentRegistrationRepository;
import io.arpicode.leagueapi.tournament.TournamentRepository;
import io.arpicode.leagueapi.tournament.dto.TournamentSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameMatchService {

    private final Clock clock;
    private final GameMatchRepository gameMatchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;

    public GameMatchService(
        Clock clock,
        GameMatchRepository gameMatchRepository,
        TournamentRepository tournamentRepository,
        TournamentRegistrationRepository tournamentRegistrationRepository
    ) {
        this.clock = clock;
        this.gameMatchRepository = gameMatchRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentRegistrationRepository = tournamentRegistrationRepository;
    }

    // The tournament lock keeps every check below true until the insert: the status, the dates and
    // the roster can only change through requests that take the same lock.
    @Transactional
    public GameMatchResponse create(GameMatchCreateRequest gameMatchCreateRequest) {
        Tournament tournament = findTournamentForUpdate(gameMatchCreateRequest.tournamentId());

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
        return instant.atZoneSameInstant(clock.getZone()).toLocalDate();
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

    private GameMatchResponse toGameMatchResponse(GameMatch gameMatch) {
        Tournament tournament = gameMatch.getTournament();
        BoardGame boardGame = tournament.getBoardGame();

        return new GameMatchResponse(
            gameMatch.getId(),
            new TournamentSummary(
                tournament.getId(),
                new BoardGameSummary(
                    boardGame.getId(),
                    boardGame.getName()
                ),
                tournament.getName(),
                tournament.getStatus(),
                tournament.getMaxPlayers(),
                tournament.getStartsOn(),
                tournament.getEndsOn()
            ),
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
