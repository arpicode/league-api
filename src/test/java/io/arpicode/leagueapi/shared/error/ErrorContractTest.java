package io.arpicode.leagueapi.shared.error;

import io.arpicode.leagueapi.ApiIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Errors raised by Spring rather than by this application's own code. Each pins the status to
// the code a client would switch on, and that the code/errorId contract holds for responses this
// application never builds itself. The system under test is GlobalExceptionHandler, not either
// controller, so these live here rather than duplicated across every controller test.
//
// The four parameterized cases enter the handler through a controller's own mapping: the advice is
// global, but routing into it is not, so a controller declaring @PathVariable String id, narrowing
// consumes, or carrying a local @ExceptionHandler would break the contract for its paths alone.
// The two unparameterized cases touch no controller path, so a second copy would prove nothing.
//
// No @Transactional: every request here is rejected before anything is written.
@ApiIntegrationTest
class ErrorContractTest {

    // One entry per controller. A new controller adds a line here.
    private static final String BOARD_GAMES = "/api/v1/boardgames";
    private static final String PLAYERS = "/api/v1/players";

    @Autowired
    MockMvc mockMvc;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {BOARD_GAMES, PLAYERS})
    @DisplayName("should return 400 Bad Request with the error contract when the JSON body is malformed")
    void malformedJson(String basePath) throws Exception {
        mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {BOARD_GAMES, PLAYERS})
    @DisplayName("should return 400 Bad Request with the error contract when the id is not a number")
    void nonNumericId(String basePath) throws Exception {
        mockMvc.perform(get(basePath + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {BOARD_GAMES, PLAYERS})
    @DisplayName("should return 405 Method Not Allowed with the error contract for an unsupported method")
    void unsupportedMethod(String basePath) throws Exception {
        mockMvc.perform(patch(basePath + "/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.name()))
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {BOARD_GAMES, PLAYERS})
    @DisplayName("should return 415 Unsupported Media Type with the error contract for a non-JSON body")
    void unsupportedMediaType(String basePath) throws Exception {
        mockMvc.perform(post(basePath)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("test_user"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name()))
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

}
