package io.arpicode.leagueapi.gamematch;

import io.arpicode.leagueapi.ApiIntegrationTest;
import io.arpicode.leagueapi.boardgame.BoardGameFixtures;
import io.arpicode.leagueapi.player.PlayerFixtures;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.TournamentFixtures;
import io.arpicode.leagueapi.tournament.TournamentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiIntegrationTest
@Transactional
class GameMatchControllerTest {

    // Paris is at UTC+2 throughout October 2026, so 22:00 UTC is midnight there. The schedule tests
    // send UTC instants a minute either side of the first and last day's edges in the league's zone.
    private static final LocalDate STARTS_ON = LocalDate.of(2026, 10, 5);
    private static final LocalDate ENDS_ON = LocalDate.of(2026, 10, 10);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerFixtures players;

    @Autowired
    private BoardGameFixtures boardGames;

    @Autowired
    private TournamentFixtures tournaments;

    // -- Create

    @Test
    @DisplayName("should create a game match seating the players in the order the request lists them")
    void createGameMatch() throws Exception {
        long firstRegisteredId = players.create();
        long secondRegisteredId = players.create("test_player_username", "test_player_email@test.com");
        long notListedId = players.create();
        long tournamentId = tournaments.createInProgress(firstRegisteredId, secondRegisteredId, notListedId);

        // Listed against registration order, so a seating taken from the roster lookup's order would
        // show here. The third registered player is left out and must not be seated.
        postGameMatch(tournamentId, null, secondRegisteredId, firstRegisteredId)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(GameMatchStatus.SCHEDULED.name()))
            .andExpect(jsonPath("$.participants.length()").value(2))
            .andExpect(jsonPath("$.participants[0].seatNumber").value(1))
            .andExpect(jsonPath("$.participants[0].player.id").value(secondRegisteredId))
            .andExpect(jsonPath("$.participants[0].player.username").value("test_player_username"))
            .andExpect(jsonPath("$.participants[1].seatNumber").value(2))
            .andExpect(jsonPath("$.participants[1].player.id").value(firstRegisteredId));
    }

    @Test
    @DisplayName("should return 409 Conflict when the tournament is not in progress, before looking at its roster")
    void createGameMatchTournamentNotInProgress() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createDraft();

        // A draft has no roster, so the player is unregistered as well: the code shows which check ran.
        postGameMatch(tournamentId, null, playerId)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_IN_PROGRESS.name()))
            .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_IN_PROGRESS_FOR_GAME_MATCH.formatted(TournamentStatus.DRAFT)));
    }

    @Test
    @DisplayName("should return 409 Conflict naming the players who are not registered for the tournament")
    void createGameMatchWithUnregisteredPlayer() throws Exception {
        long registeredId = players.create();
        long registeredElsewhereId = players.create();
        long tournamentId = tournaments.createInProgress(registeredId);
        tournaments.createInProgress(registeredElsewhereId);

        // Registered in another tournament, so a lookup that ignored the tournament would find them.
        postGameMatch(tournamentId, null, registeredId, registeredElsewhereId)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ErrorCode.GAME_MATCH_PLAYERS_NOT_REGISTERED.name()))
            .andExpect(jsonPath("$.detail").value(UserMessages.GAME_MATCH_PLAYERS_NOT_REGISTERED.formatted(List.of(registeredElsewhereId), tournamentId)));
    }

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = {2, 3})
    @DisplayName("should create a game match with exactly the fewest or the most players the game takes")
    void createGameMatchAtPlayerCountBounds(int playerCount) throws Exception {
        long boardGameId = boardGames.create("test_board_game_name", 2, 3);
        long[] playerIds = newPlayers(playerCount);
        long tournamentId = tournaments.createInProgress(boardGameId, null, null, playerIds);

        postGameMatch(tournamentId, null, playerIds)
            .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = {1, 4})
    @DisplayName("should return 409 Conflict when the game takes more or fewer players than listed")
    void createGameMatchOutsidePlayerCountBounds(int playerCount) throws Exception {
        long boardGameId = boardGames.create("test_board_game_name", 2, 3);
        long[] playerIds = newPlayers(playerCount);
        long tournamentId = tournaments.createInProgress(boardGameId, null, null, playerIds);

        postGameMatch(tournamentId, null, playerIds)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ErrorCode.GAME_MATCH_PLAYER_COUNT_OUT_OF_RANGE.name()))
            .andExpect(jsonPath("$.detail").value(UserMessages.GAME_MATCH_PLAYER_COUNT_OUT_OF_RANGE.formatted("test_board_game_name", 2, 3, playerCount)));
    }

    @Test
    @DisplayName("should return 400 Bad Request when a player is listed twice")
    void createGameMatchWithDuplicatePlayer() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createInProgress(playerId);

        // Registered, and two seats fit the default game, so the duplicate is the only fault.
        postGameMatch(tournamentId, null, playerId, playerId)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
            .andExpect(jsonPath("$.errors[0].field").value("playerIds"))
            .andExpect(jsonPath("$.errors[0].message").value("A player can only take one seat"));
    }

    // 22:00 UTC on the 4th is midnight on the 5th in Paris, the first day although the UTC day is the
    // day before. 21:59 UTC on the 10th is 23:59 in Paris, the last minute of the last day.
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"2026-10-04T22:00:00Z", "2026-10-10T21:59:00Z"})
    @DisplayName("should create a game match scheduled on the tournament's first or last day in the league's zone")
    void createGameMatchOnTournamentFirstOrLastDay(String scheduledAt) throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createInProgress(boardGames.create(), STARTS_ON, ENDS_ON, playerId);

        postGameMatch(tournamentId, scheduledAt, playerId)
            .andExpect(status().isCreated());
    }

    // 21:59 UTC on the 4th is still the 4th in Paris. 22:00 UTC on the 10th is already the 11th in
    // Paris, although the UTC day is the last one.
    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "2026-10-04T21:59:00Z, 2026-10-04",
        "2026-10-10T22:00:00Z, 2026-10-11"
    })
    @DisplayName("should return 409 Conflict when the game match falls outside the tournament's dates in the league's zone")
    void createGameMatchOutsideTournamentDates(String scheduledAt, LocalDate leagueDay) throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createInProgress(boardGames.create(), STARTS_ON, ENDS_ON, playerId);

        postGameMatch(tournamentId, scheduledAt, playerId)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ErrorCode.GAME_MATCH_OUTSIDE_TOURNAMENT_DATES.name()))
            .andExpect(jsonPath("$.detail").value(UserMessages.GAME_MATCH_OUTSIDE_TOURNAMENT_DATES.formatted(leagueDay)));
    }

    @Test
    @DisplayName("should keep the schedule of a game match in a tournament without dates")
    void createGameMatchScheduledInTournamentWithoutDates() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createInProgress(playerId);

        postGameMatch(tournamentId, "2026-10-10T18:00:00Z", playerId)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.scheduledAt").value("2026-10-10T18:00:00Z"));
    }

    private ResultActions postGameMatch(long tournamentId, String scheduledAt, long... playerIds) throws Exception {
        return mockMvc.perform(post("/api/v1/gamematches")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"tournamentId":%d, "scheduledAt":%s, "playerIds":%s}
                """.formatted(
                tournamentId,
                scheduledAt == null ? "null" : "\"" + scheduledAt + "\"",
                Arrays.toString(playerIds))));
    }

    private long[] newPlayers(int count) throws Exception {
        long[] playerIds = new long[count];
        for (int i = 0; i < count; i++) {
            playerIds[i] = players.create();
        }
        return playerIds;
    }

}
