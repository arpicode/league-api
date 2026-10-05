package io.arpicode.leagueapi.tournament.match;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private GameMatchFixtures gameMatches;

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
        MockHttpServletResponse response = postGameMatch(tournamentId, null, secondRegisteredId, firstRegisteredId)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tournamentId").value(tournamentId))
            .andExpect(jsonPath("$.status").value(GameMatchStatus.SCHEDULED.name()))
            .andExpect(jsonPath("$.participants.length()").value(2))
            .andExpect(jsonPath("$.participants[0].seatNumber").value(1))
            .andExpect(jsonPath("$.participants[0].player.id").value(secondRegisteredId))
            .andExpect(jsonPath("$.participants[0].player.username").value("test_player_username"))
            .andExpect(jsonPath("$.participants[1].seatNumber").value(2))
            .andExpect(jsonPath("$.participants[1].player.id").value(firstRegisteredId))
            .andReturn().getResponse();

        long id = ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();

        assertThat(response.getHeader("Location"))
            .as("Location of the created game match")
            .isEqualTo("http://localhost/api/v1/tournaments/%d/matches/%d".formatted(tournamentId, id));
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

    // -- Get

    @Test
    @DisplayName("should get a game match of the tournament with its participants")
    void getGameMatch() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createInProgress(playerId);
        long id = gameMatches.create(tournamentId, playerId);

        getGameMatch(tournamentId, id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.participants[0].player.id").value(playerId));
    }

    @Test
    @DisplayName("should return 404 Not Found when the game match belongs to another tournament")
    void getGameMatchFromAnotherTournament() throws Exception {
        long playerId = players.create();
        long id = gameMatches.create(tournaments.createInProgress(playerId), playerId);
        long otherTournamentId = tournaments.createInProgress();

        // The id exists, only not under this tournament: a lookup by id alone would find it.
        getGameMatch(otherTournamentId, id)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(ErrorCode.GAME_MATCH_NOT_FOUND.name()))
            .andExpect(jsonPath("$.detail").value(UserMessages.GAME_MATCH_NOT_FOUND.formatted(id, otherTournamentId)));
    }

    @Test
    @DisplayName("should return 404 Not Found when getting a game match for a tournament that does not exist")
    void getGameMatchTournamentNotFound() throws Exception {
        // A real match id, so the only thing missing is the tournament.
        long playerId = players.create();
        long id = gameMatches.create(tournaments.createInProgress(playerId), playerId);

        getGameMatch(Long.MAX_VALUE, id)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
            .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    // -- List

    @Test
    @DisplayName("should list only the game matches of the targeted tournament")
    void listGameMatchesForTargetedTournament() throws Exception {
        long otherPlayerId = players.create();
        gameMatches.create(tournaments.createInProgress(otherPlayerId), otherPlayerId);
        long playerId = players.create();
        long tournamentId = tournaments.createInProgress(playerId);
        long id = gameMatches.create(tournamentId, playerId);

        listGameMatches(tournamentId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(id));
    }

    @Test
    @DisplayName("should list a tournament's game matches by schedule, unscheduled ones last")
    void listGameMatchesBySchedule() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createInProgress(playerId);
        // Created against schedule order, so a list in creation order would show here.
        long unscheduledId = gameMatches.create(tournamentId, playerId);
        long laterId = gameMatches.create(tournamentId, OffsetDateTime.parse("2026-10-12T18:00:00Z"), playerId);
        long earlierId = gameMatches.create(tournamentId, OffsetDateTime.parse("2026-10-11T18:00:00Z"), playerId);

        listGameMatches(tournamentId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(earlierId))
            .andExpect(jsonPath("$.content[1].id").value(laterId))
            .andExpect(jsonPath("$.content[2].id").value(unscheduledId));
    }

    @Test
    @DisplayName("should list each game match with its own participants in seat order")
    void listGameMatchesWithTheirParticipants() throws Exception {
        long firstPlayerId = players.create();
        long secondPlayerId = players.create();
        long thirdPlayerId = players.create();
        long tournamentId = tournaments.createInProgress(firstPlayerId, secondPlayerId, thirdPlayerId);
        // Seated against id order, so participants sorted by player rather than by seat would show here.
        gameMatches.create(tournamentId, secondPlayerId, firstPlayerId);
        gameMatches.create(tournamentId, thirdPlayerId);

        listGameMatches(tournamentId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].participants.length()").value(2))
            .andExpect(jsonPath("$.content[0].participants[0].player.id").value(secondPlayerId))
            .andExpect(jsonPath("$.content[0].participants[1].player.id").value(firstPlayerId))
            .andExpect(jsonPath("$.content[1].participants.length()").value(1))
            .andExpect(jsonPath("$.content[1].participants[0].player.id").value(thirdPlayerId));
    }

    @Test
    @DisplayName("should list no game matches for a tournament that has none")
    void listGameMatchesWithoutGameMatches() throws Exception {
        long tournamentId = tournaments.createInProgress();

        listGameMatches(tournamentId)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 404 Not Found when listing the game matches of a tournament that does not exist")
    void listGameMatchesTournamentNotFound() throws Exception {
        listGameMatches(Long.MAX_VALUE)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
            .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    private ResultActions postGameMatch(long tournamentId, String scheduledAt, long... playerIds) throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments/{id}/matches", tournamentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"scheduledAt":%s, "playerIds":%s}
                """.formatted(
                scheduledAt == null ? "null" : "\"" + scheduledAt + "\"",
                Arrays.toString(playerIds))));
    }

    private ResultActions getGameMatch(long tournamentId, long id) throws Exception {
        return mockMvc.perform(get("/api/v1/tournaments/{id}/matches/{matchId}", tournamentId, id));
    }

    private ResultActions listGameMatches(long tournamentId) throws Exception {
        return mockMvc.perform(get("/api/v1/tournaments/{id}/matches", tournamentId));
    }

    private long[] newPlayers(int count) throws Exception {
        long[] playerIds = new long[count];
        for (int i = 0; i < count; i++) {
            playerIds[i] = players.create();
        }
        return playerIds;
    }

}
