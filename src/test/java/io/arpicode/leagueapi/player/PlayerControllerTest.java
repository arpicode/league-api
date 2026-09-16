package io.arpicode.leagueapi.player;

import com.jayway.jsonpath.JsonPath;
import io.arpicode.leagueapi.TestcontainersConfiguration;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class PlayerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("should create a new player when valid data is provided")
    void createPlayer() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"test_user@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.email").value("test_user@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should create a new normalized player when valid data is provided")
    void createPlayerNormalized() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"  Test_User ","email":"  Test_User@Example.Com "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("Test_User"))
                .andExpect(jsonPath("$.email").value("test_user@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should return 409 Conflict when trying to create a player with duplicate username")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = "DELETE FROM league.player WHERE username IN ('test_user', 'test_user_unique')", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPlayerWithDuplicateUsername() throws Exception {
        // First creation should succeed
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"test_user@example.com"}
                                """))
                .andExpect(status().isCreated());

        // Second creation should fail with 409 Conflict due to duplicate username
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"test_user_unique@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.USERNAME_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 409 Conflict when trying to create a player with duplicate email")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = "DELETE FROM league.player WHERE username IN ('test_user', 'test_user_unique')", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPlayerWithDuplicateEmail() throws Exception {
        // First creation should succeed
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"test_user@example.com"}
                                """))
                .andExpect(status().isCreated());

        // Second creation should fail with 409 Conflict due to duplicate email
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user_unique","email":"test_user@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.EMAIL_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 409 Conflict when trying to create a player with duplicate email (case insensitive)")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = "DELETE FROM league.player WHERE username IN ('test_user', 'test_user_unique')", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPlayerWithDuplicateEmailCaseInsensitive() throws Exception {
        // First creation should succeed
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"test_user@example.com"}
                                """))
                .andExpect(status().isCreated());

        // Second creation should fail with 409 Conflict due to duplicate email (case insensitive)
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user_unique","email":"TEST_USER@EXAMPLE.COM"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.EMAIL_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with username that is too short")
    void createPlayerWithInvalidDataTooShortUsername() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"te","email":"test_user@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("username"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Username must be between 3 and 50 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with username that is too long")
    void createPlayerWithInvalidDataTooLongUsername() throws Exception {
        String longUsername = "t".repeat(51);
        mockMvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"test_user@example.com"}
                                """.formatted(longUsername)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("username"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Username must be between 3 and 50 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with a blank username")
    void createPlayerWithInvalidDataBlankUsername() throws Exception {
        mockMvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"test_user@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("username"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Username cannot be blank"))
                .andExpect(jsonPath("$.errors[1].message")
                        .value("Username must be between 3 and 50 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with non valid email")
    void createPlayerWithInvalidDataNonValidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"test_user_at_example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("Email should be valid"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with a blank email")
    void createPlayerWithInvalidDataBlankEmail() throws Exception {
        mockMvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Email cannot be blank"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when the email is well-formed but longer than the column allows")
    void createPlayerWithInvalidDataTooLongEmail() throws Exception {
        // Well-formed but longer than 255 characters: 60 + 1 + (63 + 1) * 3 + 3 = 256
        String longEmail = "a".repeat(60) + "@" + ("b".repeat(63) + ".").repeat(3) + "com";

        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"%s"}
                                """.formatted(longEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Email must not exceed 255 characters"));
    }

    @Test
    @DisplayName("should return a list of all the players")
    void listAllPlayers() throws Exception {
        for (int i = 0; i < 3; i++) {
            String username = "test_user_" + i;
            String email = "test_user_" + i + "@example.com";
            mockMvc.perform(post("/api/v1/players")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","email":"%s"}
                                    """.formatted(username, email)))
                    .andExpect(status().isCreated());
        }

        // Asserting the usernames (not just the count) so that returning the wrong
        // three rows fails; containsInAnyOrder also pins the size, and stays valid
        // while findAll() is unordered.
        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username",
                        containsInAnyOrder("test_user_0", "test_user_1", "test_user_2")));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to get a player that does not exist")
    void getPlayerNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/players/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(999)))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("should update an existing player when valid data is provided")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = "DELETE FROM league.player WHERE username IN ('test_user', 'updated_user')", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updatePlayer() throws Exception {
        // Create and update each commit in their own transaction (like real requests do),
        // since Postgres now() is frozen for the life of a transaction and would otherwise
        // make updatedAt look unchanged even when the update trigger fires correctly.
        MockHttpServletResponse createResponse = mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"test_user@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");
        String createdAt = JsonPath.read(createResponse.getContentAsString(), "$.createdAt");
        String updatedAt = JsonPath.read(createResponse.getContentAsString(), "$.updatedAt");
        OffsetDateTime oldUpdatedAt = OffsetDateTime.parse(updatedAt);

        // Update the newly created player
        MockHttpServletResponse updateResponse = mockMvc.perform(put("/api/v1/players/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"updated_user","email":"updated_user@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.username").value("updated_user"))
                .andExpect(jsonPath("$.email").value("updated_user@example.com"))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andReturn().getResponse();

        String newUpdatedAt = JsonPath.read(updateResponse.getContentAsString(), "$.updatedAt");
        OffsetDateTime newUpdatedAtParsed = OffsetDateTime.parse(newUpdatedAt);

        assertTrue(newUpdatedAtParsed.isAfter(oldUpdatedAt));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to update a player that does not exist")
    void updatePlayerNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/players/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"updated_user","email":"updated_user@example.com"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(999)));
    }

    @Test
    @DisplayName("should delete an existing player")
    void deletePlayer() throws Exception {
        MockHttpServletResponse createResponse = mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","email":"test_user@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/v1/players/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/players/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail")
                        .value(UserMessages.PLAYER_NOT_FOUND.formatted(id.intValue())));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to delete a player that does not exist")
    void deletePlayerNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/players/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(999)));
    }

}