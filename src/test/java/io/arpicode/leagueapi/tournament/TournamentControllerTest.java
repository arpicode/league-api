package io.arpicode.leagueapi.tournament;

import com.jayway.jsonpath.JsonPath;
import io.arpicode.leagueapi.ApiIntegrationTest;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ApiIntegrationTest
@Transactional
class TournamentControllerTest {

    // Matches on the generated name_normalized rather than name, so a row committed under a
    // different casing by a failing test is removed too.
    private static final String CLEANUP_TOURNAMENTS =
            "DELETE FROM league.tournament WHERE name_normalized IN ('test_tournament_name', 'updated_test_tournament_name', 'test_other_tournament_name')";
    private static final String CLEANUP_BOARD_GAMES =
            "DELETE FROM league.board_game WHERE name_normalized IN ('test_board_game_name', 'test_other_board_game')";


    @Autowired
    MockMvc mockMvc;

    @Test
    void createTournament() throws Exception {
        Long boardGameId = createBoardGame();

        MockHttpServletResponse createResponse = postTournament(boardGameId,
                "test_tournament_name",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.boardGame.id").value(boardGameId))
                .andExpect(jsonPath("$.boardGame.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.name").value("test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.DRAFT.name()))
                .andExpect(jsonPath("$.maxPlayers").value(16))
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn().getResponse();

        String createdAt = JsonPath.read(createResponse.getContentAsString(), "$.createdAt");
        String updatedAt = JsonPath.read(createResponse.getContentAsString(), "$.updatedAt");

        assertThat(OffsetDateTime.parse(createdAt)).isEqualTo(OffsetDateTime.parse(updatedAt));
    }

    @Test
    @DisplayName("should create a new tournament when valid data is provided keeping the name casing")
    void createTournamentWithCaseSensitiveName() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId,
                "test_tournament_name_CASE",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.boardGame.id").value(boardGameId))
                .andExpect(jsonPath("$.boardGame.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.name").value("test_tournament_name_CASE"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.DRAFT.name()))
                .andExpect(jsonPath("$.maxPlayers").value(16))
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should create a new tournament when valid data with no optional fields is provided")
    void createTournamentWithNoOptionalFields() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId,
                "test_tournament_name")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.boardGame.id").value(boardGameId))
                .andExpect(jsonPath("$.boardGame.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.name").value("test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.DRAFT.name()))
                .andExpect(jsonPath("$.maxPlayers").value(nullValue()))
                .andExpect(jsonPath("$.startsOn").value(nullValue()))
                .andExpect(jsonPath("$.endsOn").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should create a new tournament when a start date and no end date are provided")
    void createTournamentWithStartDateAndNoEndDate() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId,
                "test_tournament_name",
                LocalDate.of(2000, 1, 1),
                null)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.boardGame.id").value(boardGameId))
                .andExpect(jsonPath("$.boardGame.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.name").value("test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.DRAFT.name()))
                .andExpect(jsonPath("$.maxPlayers").value(nullValue()))
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    // -- Create: uniqueness

    @Test
    @DisplayName("should return 409 Conflict when trying to create a tournament with duplicate name")
    void createTournamentWithDuplicateName() throws Exception {
        Long boardGameId = createBoardGame();
        createTournament(boardGameId, "test_tournament_name");

        postTournament(boardGameId, "test_tournament_name")
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NAME_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NAME_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 409 Conflict when trying to create a tournament with duplicate differing only by case")
    void createTournamentWithDuplicateNameCaseInsensitive() throws Exception {
        Long boardGameId = createBoardGame();
        createTournament(boardGameId, "test_tournament_name");

        postTournament(boardGameId, "TEST_TOURNAMENT_NAME")
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NAME_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NAME_ALREADY_EXISTS));
    }

    // -- Create: field validation

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a tournament with name that is too short")
    void createTournamentWithInvalidDataTooShortName() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId, "t")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Name must be between 2 and 150 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a tournament with name that is too long")
    void createTournamentWithInvalidDataTooLongName() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId, "t".repeat(151))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Name must be between 2 and 150 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a tournament with a blank name")
    void createTournamentWithInvalidDataBlankName() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId, "")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Name cannot be blank"))
                .andExpect(jsonPath("$.errors[1].message")
                        .value("Name must be between 2 and 150 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a tournament with max players lower than 2")
    void createTournamentWithInvalidDataWithTooLowMaxPlayers() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId,
                "test_tournament_name",
                1,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("maxPlayers"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Maximum players must be at least 2"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a tournament with an end date without a start date")
    void createTournamentWithInvalidDatesEndWithoutStart() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId,
                "test_tournament_name",
                null,
                LocalDate.of(2000, 1, 1))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("startsOn"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("A start date is required when an end date is set"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a tournament with an end date before a start date")
    void createTournamentWithInvalidDatesEndBeforeStart() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId,
                "test_tournament_name",
                LocalDate.of(2000, 1, 25),
                LocalDate.of(2000, 1, 1))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("endsOn"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("End date can't be before the start date"));
    }

    // -- Read

    @Test
    @DisplayName("should return a tournament by id")
    void getTournamentById() throws Exception {
        Long boardGameId = createBoardGame();
        Long id = createTournament(
                boardGameId,
                "test_tournament_name",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1)
        );

        mockMvc.perform(get("/api/v1/tournaments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardGame.id").value(boardGameId))
                .andExpect(jsonPath("$.boardGame.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.boardGame.minPlayers").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.boardGame.maxPlayers").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.name").value("test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.DRAFT.name()))
                .andExpect(jsonPath("$.maxPlayers").value(16))
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should return a list of all the tournaments sorted by normalized names")
    void listAllTournamentsSorted() throws Exception {
        Long boardGameId = createBoardGame();
        createTournament(
                boardGameId,
                "test_tournament_name_2",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1)
        );

        createTournament(
                boardGameId,
                "test_tournament_name_1",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1)
        );

        createTournament(
                boardGameId,
                "test_tournament_name_3",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1)
        );

        mockMvc.perform(get("/api/v1/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name",
                        contains("test_tournament_name_1", "test_tournament_name_2", "test_tournament_name_3")))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @DisplayName("should honour the requested tournament page and size")
    void listTournamentsSecondPage() throws Exception {
        Long boardGameId = createBoardGame();

        createTournament(
                boardGameId,
                "test_tournament_name_3",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1)
        );
        createTournament(
                boardGameId,
                "test_tournament_name_2",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1)
        );
        createTournament(
                boardGameId,
                "test_tournament_name_1",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1)
        );

        mockMvc.perform(get("/api/v1/tournaments?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("test_tournament_name_3"))
                .andExpect(jsonPath("$.page.number").value(1)) // Second page (0 indexed)
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to get a board game that does not exist")
    void getBoardGameNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/tournaments/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    // -- Update

    @Test
    @DisplayName("should update an existing tournament when valid data is provided")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = {CLEANUP_TOURNAMENTS, CLEANUP_BOARD_GAMES}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateTournament() throws Exception {
        // Create and update each commit in their own transaction (like real requests do),
        // since Postgres now() is frozen for the life of a transaction and would otherwise
        // make updatedAt look unchanged even when the update trigger fires correctly.
        Long boardGameId = createBoardGame();

        MockHttpServletResponse createResponse = postTournament(
                boardGameId,
                "test_tournament_name"
        )
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");
        String createdAt = JsonPath.read(createResponse.getContentAsString(), "$.createdAt");
        OffsetDateTime oldUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(createResponse.getContentAsString(), "$.updatedAt"));

        MockHttpServletResponse updateResponse = mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"updated_test_tournament_name", "status":"%s", "maxPlayers":"16", "startsOn":"%s", "endsOn":"%s"}
                                """.formatted(boardGameId, TournamentStatus.OPEN, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.name").value("updated_test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.OPEN.name()))
                .andExpect(jsonPath("$.maxPlayers").value(16))
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andReturn().getResponse();

        OffsetDateTime newUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(updateResponse.getContentAsString(), "$.updatedAt"));

        assertThat(newUpdatedAt).isAfter(oldUpdatedAt);
    }

    @Test
    @DisplayName("should update an existing tournament that has a draft status when valid data is provided")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = {CLEANUP_TOURNAMENTS, CLEANUP_BOARD_GAMES}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateTournamentInDraftStatus() throws Exception {
        // Create and update each commit in their own transaction (like real requests do),
        // since Postgres now() is frozen for the life of a transaction and would otherwise
        // make updatedAt look unchanged even when the update trigger fires correctly.
        Long boardGameId = createBoardGame();

        MockHttpServletResponse createResponse = postTournament(
                boardGameId,
                "test_tournament_name"
        )
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");
        String createdAt = JsonPath.read(createResponse.getContentAsString(), "$.createdAt");
        OffsetDateTime oldUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(createResponse.getContentAsString(), "$.updatedAt"));

        MockHttpServletResponse updateResponse = mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"updated_test_tournament_name", "status":"%s", "maxPlayers":"16", "startsOn":"%s", "endsOn":"%s"}
                                """.formatted(boardGameId, TournamentStatus.DRAFT, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.name").value("updated_test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.DRAFT.name()))
                .andExpect(jsonPath("$.maxPlayers").value(16))
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andReturn().getResponse();

        OffsetDateTime newUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(updateResponse.getContentAsString(), "$.updatedAt"));

        assertThat(newUpdatedAt).isAfter(oldUpdatedAt);
    }

    @Test
    @DisplayName("should allow changing the board game on a tournament that has a draft status")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = {CLEANUP_TOURNAMENTS, CLEANUP_BOARD_GAMES}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void changeBoardGameOnDraftTournament() throws Exception {
        // Create and update each commit in their own transaction (like real requests do),
        // since Postgres now() is frozen for the life of a transaction and would otherwise
        // make updatedAt look unchanged even when the update trigger fires correctly.
        Long boardGameId = createBoardGame();
        Long otherBoardGameId = createBoardGame("test_other_board_game");

        MockHttpServletResponse createResponse = postTournament(
                boardGameId,
                "test_tournament_name"
        )
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");
        String createdAt = JsonPath.read(createResponse.getContentAsString(), "$.createdAt");
        OffsetDateTime oldUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(createResponse.getContentAsString(), "$.updatedAt"));

        MockHttpServletResponse updateResponse = mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"updated_test_tournament_name", "status":"%s", "maxPlayers":"16", "startsOn":"%s", "endsOn":"%s"}
                                """.formatted(otherBoardGameId, TournamentStatus.DRAFT, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.name").value("updated_test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.DRAFT.name()))
                .andExpect(jsonPath("$.maxPlayers").value(16))
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andReturn().getResponse();

        OffsetDateTime newUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(updateResponse.getContentAsString(), "$.updatedAt"));

        assertThat(newUpdatedAt).isAfter(oldUpdatedAt);
    }

    @Test
    @DisplayName("should return 409 Conflict when changing the board game on a tournament that has a open status")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = {CLEANUP_TOURNAMENTS, CLEANUP_BOARD_GAMES}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void changeBoardGameOnOpenTournament() throws Exception {
        Long boardGameId = createBoardGame();
        Long otherBoardGameId = createBoardGame("test_other_board_game");

        MockHttpServletResponse createResponse = postTournament(
                boardGameId,
                "test_tournament_name"
        )
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");

        // transition to OPEN
        mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"updated_test_tournament_name", "status":"%s", "maxPlayers":"16", "startsOn":"%s", "endsOn":"%s"}
                                """.formatted(boardGameId, TournamentStatus.OPEN, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1))))
                .andExpect(status().isOk());

        // change the game
        mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"updated_test_tournament_name", "status":"%s", "maxPlayers":"16", "startsOn":"%s", "endsOn":"%s"}
                                """.formatted(otherBoardGameId, TournamentStatus.OPEN, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1))))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_BOARD_GAME_LOCKED.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_BOARD_GAME_LOCKED.formatted(TournamentStatus.OPEN)));
    }

    @Test
    @DisplayName("should return 409 Conflict when transitioning to an illegal stats")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = {CLEANUP_TOURNAMENTS, CLEANUP_BOARD_GAMES}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void transitionToIllegalStatus() throws Exception {
        Long boardGameId = createBoardGame();

        MockHttpServletResponse createResponse = postTournament(
                boardGameId,
                "test_tournament_name"
        )
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");

        // illegal transition from DRAFT to IN_PROGRESS
        mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"updated_test_tournament_name", "status":"%s", "maxPlayers":"16", "startsOn":"%s", "endsOn":"%s"}
                                """.formatted(boardGameId, TournamentStatus.IN_PROGRESS, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1))))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_ILLEGAL_TRANSITION.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_ILLEGAL_TRANSITION.formatted(TournamentStatus.DRAFT, TournamentStatus.IN_PROGRESS)));
    }

    @Test
    @DisplayName("should allow changing the board game while opening a tournament in the same request")
    void changeBoardGameWhileTransitioningToOpen() throws Exception {
        // changeBoardGame() validates against the status the tournament had when the request
        // arrived, so repointing a DRAFT tournament and opening it in a single call is allowed.
        // Applying the transition first would make this very request fail as BOARD_GAME_LOCKED.
        Long boardGameId = createBoardGame();
        long otherBoardGameId = createBoardGame("test_other_board_game");
        long id = createTournament(boardGameId, "test_tournament_name");

        putTournament(id, otherBoardGameId, "test_tournament_name", TournamentStatus.OPEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardGame.id").value(otherBoardGameId))
                .andExpect(jsonPath("$.boardGame.name").value("test_other_board_game"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.OPEN.name()));
    }

    @Test
    @DisplayName("should return 404 Not Found when updating a tournament with a board game that does not exist")
    void updateTournamentWithBoardGameNotFound() throws Exception {
        // The update path loads the board game rather than referencing it, so an unknown id is
        // reported as BOARD_GAME_NOT_FOUND instead of surfacing as a raw fk_tournament_board_game
        // violation once the change is flushed.
        Long boardGameId = createBoardGame();
        long id = createTournament(boardGameId, "test_tournament_name");

        putTournament(id, Long.MAX_VALUE, "test_tournament_name", TournamentStatus.DRAFT)
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_GAME_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.BOARD_GAME_NOT_FOUND.formatted(Long.MAX_VALUE)))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should clear the optional fields left out of the update request")
    void updateTournamentClearsOmittedOptionalFields() throws Exception {
        // PUT replaces the whole resource: an omitted optional field is cleared, not preserved.
        // Guarding the setters with null checks would quietly turn this endpoint into a PATCH.
        long boardGameId = createBoardGame();
        long id = createTournament(boardGameId,
                "test_tournament_name",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 2));

        putTournament(id, boardGameId, "test_tournament_name", TournamentStatus.DRAFT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxPlayers").value(nullValue()))
                .andExpect(jsonPath("$.startsOn").value(nullValue()))
                .andExpect(jsonPath("$.endsOn").value(nullValue()));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to update a tournament that does not exist")
    void updateTournamentNotFound() throws Exception {
        // A real board game id, so a reordering that resolved the game before the tournament
        // would still be reported here as the tournament being missing.
        long boardGameId = createBoardGame();

        putTournament(Long.MAX_VALUE, boardGameId, "test_tournament_name", TournamentStatus.DRAFT)
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should return 409 Conflict when renaming a tournament that has been closed")
    void updateTournamentLockedWhenClosed() throws Exception {
        Long boardGameId = createBoardGame();
        long id = createTournament(boardGameId, "test_tournament_name");

        putTournament(id, boardGameId, "test_tournament_name", TournamentStatus.OPEN)
                .andExpect(status().isOk());
        putTournament(id, boardGameId, "test_tournament_name", TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());
        putTournament(id, boardGameId, "test_tournament_name", TournamentStatus.CLOSED)
                .andExpect(status().isOk());

        putTournament(id, boardGameId, "updated_test_tournament_name", TournamentStatus.CLOSED)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_LOCKED.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_LOCKED.formatted(TournamentStatus.CLOSED)))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should accept an update of a cancelled tournament that changes nothing")
    void updateCancelledTournamentIsIdempotent() throws Exception {
        // A full-replace client reads a tournament, changes nothing and writes it back.
        // Re-sending the current values is not an edit, so the lock must not turn that into a
        // conflict -- the same rule changeBoardGame() follows.
        Long boardGameId = createBoardGame();
        long id = createTournament(boardGameId, "test_tournament_name");

        putTournament(id, boardGameId, "test_tournament_name", TournamentStatus.CANCELLED)
                .andExpect(status().isOk());

        putTournament(id, boardGameId, "test_tournament_name", TournamentStatus.CANCELLED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.CANCELLED.name()));
    }

    @Test
    @DisplayName("should allow renaming a tournament while cancelling it in the same request")
    void renameTournamentWhileCancelling() throws Exception {
        // replaceDetails() validates against the status the request arrived with, so an open
        // tournament can still be corrected by the very call that cancels it. Applying the
        // transition first would reject this request against CANCELLED.
        Long boardGameId = createBoardGame();
        long id = createTournament(boardGameId, "test_tournament_name");

        putTournament(id, boardGameId, "test_tournament_name", TournamentStatus.OPEN)
                .andExpect(status().isOk());

        putTournament(id, boardGameId, "updated_test_tournament_name", TournamentStatus.CANCELLED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated_test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.CANCELLED.name()));
    }

    // -- Delete

    @Test
    @DisplayName("should delete a tournament that is still a draft")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = {CLEANUP_TOURNAMENTS, CLEANUP_BOARD_GAMES}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deleteDraftTournament() throws Exception {
        // Committed rather than rolled back, so the DELETE really reaches Postgres and the
        // follow-up read is a fresh query. Inside the test transaction the row would only be
        // removed from the persistence context, and a database-level refusal -- a future
        // registrations FK with ON DELETE RESTRICT -- would never surface here.
        Long boardGameId = createBoardGame();
        long id = createTournament(boardGameId, "test_tournament_name");

        mockMvc.perform(delete("/api/v1/tournaments/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tournaments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(id)));
    }

    @Test
    @DisplayName("should return 409 Conflict when deleting a tournament that is no longer a draft")
    void deleteTournamentThatIsNoLongerADraft() throws Exception {
        long boardGameId = createBoardGame();
        long id = createTournament(boardGameId, "test_tournament_name");

        putTournament(id, boardGameId, "test_tournament_name", TournamentStatus.OPEN)
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/tournaments/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_DELETABLE.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_DELETABLE.formatted(TournamentStatus.OPEN)))
                .andExpect(jsonPath("$.errorId").isNotEmpty());

        // The guard must leave the row alone, not merely report a conflict on the way out.
        mockMvc.perform(get("/api/v1/tournaments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TournamentStatus.OPEN.name()));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to delete a tournament that does not exist")
    void deleteTournamentNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/tournaments/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    // -- Helpers

    private ResultActions putTournament(long id, long boardGameId, String name, TournamentStatus status) throws Exception {
        return mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"boardGameId":"%d", "name":"%s", "status":"%s"}
                        """.formatted(boardGameId, name, status)));
    }

    private ResultActions postTournament(Long boardGameId, String name, int maxPlayers, LocalDate startsOn, LocalDate endsOn) throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"boardGameId":"%d", "name":"%s", "maxPlayers":"%d", "startsOn":"%s", "endsOn":"%s"}
                        """.formatted(boardGameId, name, maxPlayers, startsOn, endsOn)));
    }

    private ResultActions postTournament(Long boardGameId, String name, LocalDate startsOn, LocalDate endsOn) throws Exception {
        String body = """
                {"boardGameId":"%d", "name":"%s", "startsOn":"%s", "endsOn":"%s"}
                """.formatted(boardGameId, name, startsOn, endsOn);
        if (startsOn == null) {
            body = """
                    {"boardGameId":"%d", "name":"%s", "endsOn":"%s"}
                    """.formatted(boardGameId, name, endsOn);
        }
        if (endsOn == null) {
            body = """
                    {"boardGameId":"%d", "name":"%s", "startsOn":"%s"}
                    """.formatted(boardGameId, name, startsOn);
        }
        return mockMvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions postTournament(Long boardGameId, String name) throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"boardGameId":"%d", "name":"%s"}
                        """.formatted(boardGameId, name)));
    }

    private long createTournament(Long boardGameId, String name, int maxPlayers, LocalDate startsOn, LocalDate endsOn) throws Exception {
        MockHttpServletResponse response = postTournament(boardGameId,
                name,
                maxPlayers,
                startsOn,
                endsOn)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

    private long createTournament(Long boardGameId, String name) throws Exception {
        MockHttpServletResponse response = postTournament(boardGameId, name)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

    private long createBoardGame() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/boardgames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"test_board_game_name", "minPlayers":"1", "maxPlayers":"4"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

    private long createBoardGame(String name) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/boardgames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s", "minPlayers":"1", "maxPlayers":"4"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }


}