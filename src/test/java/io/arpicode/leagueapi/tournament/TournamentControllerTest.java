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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ApiIntegrationTest
@Transactional
class TournamentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void createTournament() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId,
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
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
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
                .andExpect(jsonPath("$.maxPlayers").isEmpty())
                .andExpect(jsonPath("$.startsOn").isEmpty())
                .andExpect(jsonPath("$.endsOn").isEmpty())
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
                .andExpect(jsonPath("$.maxPlayers").isEmpty())
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").isEmpty())
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

    // -- Helpers

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


}