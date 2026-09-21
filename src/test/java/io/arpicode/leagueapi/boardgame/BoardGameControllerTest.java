package io.arpicode.leagueapi.boardgame;

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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ApiIntegrationTest
@Transactional
class BoardGameControllerTest {

    // Matches on the generated name_normalized rather than name, so a row committed under a
    // different casing by a failing test is removed too.
    private static final String CLEANUP =
            "DELETE FROM league.board_game WHERE name_normalized IN ('test_board_game_name', 'updated_test_board_game_name', 'test_other_board_game_name')";


    @Autowired
    MockMvc mockMvc;

    // -- Create

    @Test
    @DisplayName("should create a new board game when valid data is provided")
    void createBoardGame() throws Exception {
        postBoardGame("test_board_game_name", 1, 6, 45)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.minPlayers").value(1))
                .andExpect(jsonPath("$.maxPlayers").value(6))
                .andExpect(jsonPath("$.avgDurationMin").value(45))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should create a new board game when valid data is provided keeping the name casing")
    void createBoardGameWithCaseSensitiveName() throws Exception {
        postBoardGame("test_board_game_name_CASE", 1, 6, 45)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("test_board_game_name_CASE"))
                .andExpect(jsonPath("$.minPlayers").value(1))
                .andExpect(jsonPath("$.maxPlayers").value(6))
                .andExpect(jsonPath("$.avgDurationMin").value(45))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should create a new board game when valid data with no average duration is provided")
    void createBoardGameWithNoAverageDuration() throws Exception {
        postBoardGame("test_board_game_name", 1, 6)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.minPlayers").value(1))
                .andExpect(jsonPath("$.maxPlayers").value(6))
                .andExpect(jsonPath("$.avgDurationMin").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    // -- Create: uniqueness

    @Test
    @DisplayName("should return 409 Conflict when trying to create a board game with duplicate name")
    void createBoardGameWithDuplicateName() throws Exception {
        createBoardGame("test_board_game_name", 2, 4, 30);

        postBoardGame("test_board_game_name", 1, 6, 45)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_GAME_NAME_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.BOARD_GAME_NAME_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 409 Conflict when trying to create a board game with duplicate differing only by case")
    void createBoardGameWithDuplicateNameCaseInsensitive() throws Exception {
        createBoardGame("test_board_game_name", 1, 6, 45);

        postBoardGame("Test_Board_Game_Name", 1, 6, 45)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_GAME_NAME_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.BOARD_GAME_NAME_ALREADY_EXISTS));
    }

    // -- Create: field validation

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a board game with name that is too short")
    void createBoardGameWithInvalidDataTooShortName() throws Exception {
        postBoardGame("t", 1, 1, 10)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Name must be between 2 and 120 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a board game with name that is too long")
    void createBoardGameWithInvalidDataTooLongName() throws Exception {
        postBoardGame("t".repeat(121), 1, 1, 10)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Name must be between 2 and 120 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a board game with a blank name")
    void createBoardGameWithInvalidDataWithBlankName() throws Exception {
        postBoardGame("", 1, 1, 10)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Name cannot be blank"))
                .andExpect(jsonPath("$.errors[1].message")
                        .value("Name must be between 2 and 120 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a board game with min players lower than 1")
    void createBoardGameWithInvalidDataWithTooLowMinPlayers() throws Exception {
        postBoardGame("test_board_game_name", 0, 1, 10)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("minPlayers"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Minimum players must be at least 1"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a board game with max players lower than min players")
    void createBoardGameWithInvalidDataWithTooLowMaxPlayers() throws Exception {
        postBoardGame("test_board_game_name", 2, 1, 10)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("maxPlayers"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Maximum players must be greater than or equal to minimum players"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a board game with average duration lower than 1 min")
    void createBoardGameWithInvalidDataWithTooLowAverageDuration() throws Exception {
        postBoardGame("test_board_game_name", 1, 1, 0)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("avgDurationMin"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Average duration (minutes) must be at least 1"));
    }

    // -- Read

    @Test
    @DisplayName("should return a board game by id")
    void getBoarGameById() throws Exception {
        long id = createBoardGame("test_board_game_name", 1, 2, 3);

        mockMvc.perform(get("/api/v1/boardgames/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.minPlayers").value(1))
                .andExpect(jsonPath("$.maxPlayers").value(2))
                .andExpect(jsonPath("$.avgDurationMin").value(3))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should return a board game with no average duration by id")
    void getBoarGameByIdWithNoAverageDuration() throws Exception {
        long id = createBoardGame("test_board_game_name", 1, 2);

        mockMvc.perform(get("/api/v1/boardgames/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("test_board_game_name"))
                .andExpect(jsonPath("$.minPlayers").value(1))
                .andExpect(jsonPath("$.maxPlayers").value(2))
                .andExpect(jsonPath("$.avgDurationMin").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should return a list of all the board games")
    void listAllBoardGames() throws Exception {
        createBoardGame("test_board_game_name_1", 1, 2);
        createBoardGame("test_board_game_name_2", 1, 2);
        createBoardGame("test_board_game_name_3", 1, 2);

        mockMvc.perform(get("/api/v1/boardgames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name",
                        contains("test_board_game_name_1", "test_board_game_name_2", "test_board_game_name_3")))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @DisplayName("should honour the requested board game page and size")
    void listBoardGamesSecondPage() throws Exception {
        createBoardGame("test_board_game_name_3", 1, 2);
        createBoardGame("test_board_game_name_2", 3, 4);
        createBoardGame("test_board_game_name_1", 5, 6);

        // List should be ordered by name so should not depend on insertion order
        mockMvc.perform(get("/api/v1/boardgames?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("test_board_game_name_3"))
                .andExpect(jsonPath("$.page.number").value(1)) // Second page (0 indexed)
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to get a board game that does not exist")
    void getBoardGameNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/boardgames/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_GAME_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.BOARD_GAME_NOT_FOUND.formatted(Long.MAX_VALUE)))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should update an existing board game when valid data is provided")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateBoardGame() throws Exception {
        // Create and update each commit in their own transaction (like real requests do),
        // since Postgres now() is frozen for the life of a transaction and would otherwise
        // make updatedAt look unchanged even when the update trigger fires correctly.
        MockHttpServletResponse createResponse = postBoardGame("test_board_game_name", 1, 2)
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");
        String createdAt = JsonPath.read(createResponse.getContentAsString(), "$.createdAt");
        OffsetDateTime oldUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(createResponse.getContentAsString(), "$.updatedAt"));

        MockHttpServletResponse updateResponse = mockMvc.perform(put("/api/v1/boardgames/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "updated_test_board_game_name", "minPlayers": 2, "maxPlayers": 3, "avgDurationMin": 45}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.name").value("updated_test_board_game_name"))
                .andExpect(jsonPath("$.minPlayers").value(2))
                .andExpect(jsonPath("$.maxPlayers").value(3))
                .andExpect(jsonPath("$.avgDurationMin").value(45))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andReturn().getResponse();

        OffsetDateTime newUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(updateResponse.getContentAsString(), "$.updatedAt"));

        assertThat(newUpdatedAt).isAfter(oldUpdatedAt);
    }

    @Test
    @DisplayName("should return 409 Conflict when updating a board game to a name another board game already uses")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updateBoardGameWithDuplicateEmail() throws Exception {
        createBoardGame("test_board_game_name", 1, 2, 3);
        long otherId = createBoardGame("test_other_board_game_name", 1, 2, 3);

        // Different code path from create: the violation surfaces at saveAndFlush, not persist.
        mockMvc.perform(put("/api/v1/boardgames/{id}", otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "test_board_game_name", "minPlayers": 1, "maxPlayers": 2, "avgDurationMin": 3}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_GAME_NAME_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.BOARD_GAME_NAME_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to update a board game that does not exist")
    void updateBoardGameNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/boardgames/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "updated_test_board_game_name", "minPlayers": 1, "maxPlayers": 2, "avgDurationMin": 3}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_GAME_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.BOARD_GAME_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    @Test
    @DisplayName("should delete an existing board game")
    void deleteBoardGame() throws Exception {
        long id = createBoardGame("test_board_game_name", 1, 2, 3);

        mockMvc.perform(delete("/api/v1/boardgames/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/boardgames/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_GAME_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.BOARD_GAME_NOT_FOUND.formatted(id)));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to delete a board game that does not exist")
    void deleteBoardGameNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/boardgames/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOARD_GAME_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.BOARD_GAME_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    // -- Helpers

    private ResultActions postBoardGame(String name, int minPlayers, int maxPlayers, int avgDurationMin) throws Exception {
        return mockMvc.perform(post("/api/v1/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s", "minPlayers":"%d", "maxPlayers":"%d", "avgDurationMin":"%d"}
                        """.formatted(name, minPlayers, maxPlayers, avgDurationMin)));
    }

    private ResultActions postBoardGame(String name, int minPlayers, int maxPlayers) throws Exception {
        return mockMvc.perform(post("/api/v1/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s", "minPlayers":"%d", "maxPlayers":"%d"}
                        """.formatted(name, minPlayers, maxPlayers)));
    }

    private long createBoardGame(String name, int minPlayers, int maxPlayers, int avgDurationMin) throws Exception {
        MockHttpServletResponse response = postBoardGame(name, minPlayers, maxPlayers, avgDurationMin)
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

    private long createBoardGame(String name, int minPlayers, int maxPlayers) throws Exception {
        MockHttpServletResponse response = postBoardGame(name, minPlayers, maxPlayers)
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

}