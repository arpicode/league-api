package io.arpicode.leagueapi.player;

import com.jayway.jsonpath.JsonPath;
import io.arpicode.leagueapi.TestcontainersConfiguration;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import({TestcontainersConfiguration.class, PlayerControllerTest.ThrowingEndpointConfiguration.class})
@AutoConfigureMockMvc
@Transactional
class PlayerControllerTest {

    // Tests that provoke a constraint violation, or that need real commits, opt out of the
    // class-level test transaction and clean up with this instead. See createPlayerWithDuplicateUsername.
    private static final String CLEANUP =
            "DELETE FROM league.player WHERE username IN ('test_user', 'test_user_unique', 'other_user', 'updated_user')";

    @Autowired
    MockMvc mockMvc;

    //-- Create

    @Test
    @DisplayName("should create a new player when valid data is provided")
    void createPlayer() throws Exception {
        postPlayer("test_user", "test_user@example.com")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.email").value("test_user@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should strip and lowercase username and email before storing them")
    void createPlayerNormalized() throws Exception {
        postPlayer("  Test_User ", "  Test_User@Example.Com ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.email").value("test_user@example.com"));
    }

    //-- Create: uniqueness

    @Test
    @DisplayName("should return 409 Conflict when trying to create a player with duplicate username")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPlayerWithDuplicateUsername() throws Exception {
        createPlayer("test_user", "test_user@example.com");

        postPlayer("test_user", "test_user_unique@example.com")
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.USERNAME_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 409 Conflict for a duplicate username differing only by case")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPlayerWithDuplicateUsernameCaseInsensitive() throws Exception {
        createPlayer("test_user", "test_user@example.com");

        postPlayer("TEST_USER", "test_user_unique@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.USERNAME_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 409 Conflict when trying to create a player with duplicate email")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPlayerWithDuplicateEmail() throws Exception {
        createPlayer("test_user", "test_user@example.com");

        postPlayer("test_user_unique", "test_user@example.com")
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.EMAIL_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 409 Conflict for a duplicate email differing only by case")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void createPlayerWithDuplicateEmailCaseInsensitive() throws Exception {
        createPlayer("test_user", "test_user@example.com");

        postPlayer("test_user_unique", "TEST_USER@EXAMPLE.COM")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.EMAIL_ALREADY_EXISTS));
    }

    //-- Create: field validation

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with username that is too short")
    void createPlayerWithInvalidDataTooShortUsername() throws Exception {
        postPlayer("te", "test_user@example.com")
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
        postPlayer("t".repeat(51), "test_user@example.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("username"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Username must be between 3 and 50 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with a blank username")
    void createPlayerWithInvalidDataBlankUsername() throws Exception {
        // Two violations on the same field; the handler sorts them, so the order is a contract.
        postPlayer("", "test_user@example.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("username"))
                .andExpect(jsonPath("$.errors[0].message").value("Username cannot be blank"))
                .andExpect(jsonPath("$.errors[1].message")
                        .value("Username must be between 3 and 50 characters"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with non valid email")
    void createPlayerWithInvalidDataNonValidEmail() throws Exception {
        postPlayer("test_user", "test_user_at_example.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("Email should be valid"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when trying to create a player with a blank email")
    void createPlayerWithInvalidDataBlankEmail() throws Exception {
        postPlayer("test_user", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("Email cannot be blank"));
    }

    @Test
    @DisplayName("should return 400 Bad Request when the email is well-formed but longer than the column allows")
    void createPlayerWithInvalidDataTooLongEmail() throws Exception {
        // Well-formed but longer than 255 characters: 60 + 1 + (63 + 1) * 3 + 3 = 256.
        // Within @Email's own limits, so only @Size can reject it; without @Size this
        // reaches Postgres and the VARCHAR(255) overflow surfaces as 409 instead of 400.
        String longEmail = "a".repeat(60) + "@" + ("b".repeat(63) + ".").repeat(3) + "com";

        postPlayer("test_user", longEmail)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Email must not exceed 255 characters"));
    }

    //-- Read

    @Test
    @DisplayName("should return a player by id")
    void getPlayer() throws Exception {
        // Distinct from create/update, which build the response from a just-saved entity:
        // this is the only test that maps an entity read back from the database.
        long id = createPlayer("test_user", "test_user@example.com");

        mockMvc.perform(get("/api/v1/players/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.email").value("test_user@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("should return a list of all the players")
    void listAllPlayers() throws Exception {
        createPlayer("test_user_0", "test_user_0@example.com");
        createPlayer("test_user_1", "test_user_1@example.com");
        createPlayer("test_user_2", "test_user_2@example.com");

        // Asserting which players come back, not just how many: a count alone would pass
        // even if the endpoint returned the wrong rows or mapped the wrong fields.
        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username",
                        contains("test_user_0", "test_user_1", "test_user_2")))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @DisplayName("should honour the requested page and size")
    void listPlayersSecondPage() throws Exception {
        createPlayer("test_user_0", "test_user_0@example.com");
        createPlayer("test_user_1", "test_user_1@example.com");
        createPlayer("test_user_2", "test_user_2@example.com");

        // Ignoring the Pageable argument would return all three rows here.
        mockMvc.perform(get("/api/v1/players?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("test_user_2"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to get a player that does not exist")
    void getPlayerNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/players/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(Long.MAX_VALUE)))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    //-- Update

    @Test
    @DisplayName("should update an existing player when valid data is provided")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updatePlayer() throws Exception {
        // Create and update each commit in their own transaction (like real requests do),
        // since Postgres now() is frozen for the life of a transaction and would otherwise
        // make updatedAt look unchanged even when the update trigger fires correctly.
        MockHttpServletResponse createResponse = postPlayer("test_user", "test_user@example.com")
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        Number id = JsonPath.read(createResponse.getContentAsString(), "$.id");
        String createdAt = JsonPath.read(createResponse.getContentAsString(), "$.createdAt");
        OffsetDateTime oldUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(createResponse.getContentAsString(), "$.updatedAt"));

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

        OffsetDateTime newUpdatedAt =
                OffsetDateTime.parse(JsonPath.read(updateResponse.getContentAsString(), "$.updatedAt"));

        assertThat(newUpdatedAt).isAfter(oldUpdatedAt);
    }

    @Test
    @DisplayName("should return 409 Conflict when updating a player to an email another player already uses")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Sql(statements = CLEANUP, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void updatePlayerWithDuplicateEmail() throws Exception {
        createPlayer("test_user", "test_user@example.com");
        long otherId = createPlayer("other_user", "other_user@example.com");

        // Different code path from create: the violation surfaces at saveAndFlush, not persist.
        mockMvc.perform(put("/api/v1/players/{id}", otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"other_user","email":"test_user@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.EMAIL_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to update a player that does not exist")
    void updatePlayerNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/players/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"updated_user","email":"updated_user@example.com"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    //-- Delete

    @Test
    @DisplayName("should delete an existing player")
    void deletePlayer() throws Exception {
        long id = createPlayer("test_user", "test_user@example.com");

        mockMvc.perform(delete("/api/v1/players/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/players/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(id)));
    }

    @Test
    @DisplayName("should return 404 Not Found when trying to delete a player that does not exist")
    void deletePlayerNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/players/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    //-- Errors raised by Spring rather than by this application's own code.
    //-- Each pins the status to the code a client would switch on, and that the
    //-- code/errorId contract holds for responses this application never builds itself.

    @Test
    @DisplayName("should return 400 Bad Request with the error contract when the JSON body is malformed")
    void createPlayerWithMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should return 400 Bad Request with the error contract when the id is not a number")
    void getPlayerWithNonNumericId() throws Exception {
        mockMvc.perform(get("/api/v1/players/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should return 404 Not Found with the error contract for an unknown route")
    void unknownRoute() throws Exception {
        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.name()))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should return 405 Method Not Allowed with the error contract for an unsupported method")
    void unsupportedMethod() throws Exception {
        mockMvc.perform(patch("/api/v1/players/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.name()))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should return 415 Unsupported Media Type with the error contract for a non-JSON body")
    void unsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("test_user"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name()))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    @DisplayName("should return 500 with the error contract when an unexpected exception escapes")
    void unexpectedError() throws Exception {
        // Without the catch-all handler this falls through to Boot's BasicErrorController,
        // which answers with a different JSON shape carrying neither code nor errorId.
        mockMvc.perform(get("/api/v1/test-unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.INTERNAL_ERROR))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    //-- Helpers

    // An endpoint that fails the way a real bug would, so the catch-all handler has
    // something to catch. Registered only for this test class.
    @TestConfiguration
    static class ThrowingEndpointConfiguration {

        @RestController
        static class ThrowingEndpoint {
            @GetMapping("/api/v1/test-unexpected-error")
            void boom() {
                throw new IllegalStateException("simulated unexpected failure");
            }
        }
    }


    private ResultActions postPlayer(String username, String email) throws Exception {
        return mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s"}
                        """.formatted(username, email)));
    }

    private long createPlayer(String username, String email) throws Exception {
        MockHttpServletResponse response = postPlayer(username, email)
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

}
