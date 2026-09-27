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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ApiIntegrationTest
@Transactional
class TournamentRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // -- Create

    @Test
    @DisplayName("should create a tournament confirmed registration when there's enough room left")
    void createTournamentConfirmedRegistration() throws Exception {
        long playerId = createPlayer("test_player_username", "test_player_email@test.com");
        long boardGameId = createBoardGame("test_board_game_name", 1, 4);
        long tournamentId = createTournament(boardGameId, "test_tournament_name", 16);

        putTournament(tournamentId, boardGameId, "test_tournament_name", 16, TournamentStatus.OPEN)
                .andExpect(status().isOk());


        mockMvc.perform(post("/api/v1/tournaments/{id}/registrations", tournamentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":%d}
                                """.formatted(playerId)))
                .andExpect(status().isCreated())
                .andExpect(header().stringValues("Location", "http://localhost/api/v1/tournaments/%d/registrations/%d".formatted(tournamentId, playerId)))
                .andExpect(jsonPath("$.tournamentId").value(tournamentId))
                .andExpect(jsonPath("$.player.id").value(playerId))
                .andExpect(jsonPath("$.player.username").value("test_player_username"))
                .andExpect(jsonPath("$.player.email").doesNotExist())
                .andExpect(jsonPath("$.registeredAt").isNotEmpty())
                .andExpect(jsonPath("$.status").value(TournamentRegistrationStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.waitlistPosition").value(nullValue()));
    }

    @Test
    @DisplayName("should create for the targeted tournament a confirmed registration when there's enough room left")
    void createTournamentRegistrationForTargetedTournament() throws Exception {
        long playerId = createPlayer("test_player_username", "test_player_email@test.com");

        long tournamentAId = createTournamentWithNRegistrations("test_tournament_name_A", "test_board_game_name_A", 2, 0);
        createTournamentWithNRegistrations("test_tournament_name_B", "test_board_game_name_B", 2, 2);

        mockMvc.perform(post("/api/v1/tournaments/{id}/registrations", tournamentAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":%d}
                                """.formatted(playerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(TournamentRegistrationStatus.CONFIRMED.name()));
    }

    @Test
    @DisplayName("should create a waitlisted tournament registration when there's not enough room left")
    void createTournamentRegistrationWhenNotEnoughRoom() throws Exception {
        long playerId = createPlayer("test_player_username", "test_player_email@test.com");

        long tournamentAId = createTournamentWithNRegistrations("test_tournament_name_A", "test_board_game_name_A", 2, 2);

        mockMvc.perform(post("/api/v1/tournaments/{id}/registrations", tournamentAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":%d}
                                """.formatted(playerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(TournamentRegistrationStatus.WAITLISTED.name()))
                .andExpect(jsonPath("$.waitlistPosition").value(1));
    }

    @Test
    @DisplayName("should create a confirmed registration when the tournament has no maximum number of players")
    void createTournamentRegistrationWithoutMaxPlayers() throws Exception {
        long playerId = createPlayer("test_player_username", "test_player_email@test.com");
        long tournamentId = createTournamentWithNRegistrations("test_tournament_name", "test_board_game_name", null, 0);

        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(TournamentRegistrationStatus.CONFIRMED.name()));
    }

    @Test
    @DisplayName("should return 409 Conflict when the player is already registered for the tournament")
    void createTournamentRegistrationDuplicate() throws Exception {
        long playerId = createPlayer("test_player_username", "test_player_email@test.com");
        long tournamentId = createTournamentWithNRegistrations("test_tournament_name", "test_board_game_name", 2, 0);

        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated());

        // The detail names both ids, which only the service's explicit check can do: the
        // pk_tournament_registration mapping behind it shares the code but not the wording.
        postRegistration(tournamentId, playerId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_REGISTRATION_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_REGISTRATION_PLAYER_ALREADY_REGISTERED.formatted(playerId, tournamentId)));
    }

    @Test
    @DisplayName("should return 409 Conflict when registering for a tournament that is still a draft")
    void createTournamentRegistrationDraftTournament() throws Exception {
        long playerId = createPlayer("test_player_username", "test_player_email@test.com");
        long boardGameId = createBoardGame("test_board_game_name", 1, 4);
        long tournamentId = createTournament(boardGameId, "test_tournament_name", 2);

        postRegistration(tournamentId, playerId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_OPEN.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_OPEN.formatted(TournamentStatus.DRAFT)));
    }

    @Test
    @DisplayName("should return 409 Conflict when registering for a tournament that is already in progress")
    void createTournamentRegistrationInProgressTournament() throws Exception {
        long playerId = createPlayer("test_player_username", "test_player_email@test.com");
        long boardGameId = createBoardGame("test_board_game_name", 1, 4);
        long tournamentId = createTournament(boardGameId, "test_tournament_name", 2);

        putTournament(tournamentId, boardGameId, "test_tournament_name", 2, TournamentStatus.OPEN)
                .andExpect(status().isOk());
        putTournament(tournamentId, boardGameId, "test_tournament_name", 2, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());

        postRegistration(tournamentId, playerId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_OPEN.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_OPEN.formatted(TournamentStatus.IN_PROGRESS)));
    }

    @Test
    @DisplayName("should return 404 Not Found when registering for a tournament that does not exist")
    void createTournamentRegistrationTournamentNotFound() throws Exception {
        // A real player id, so a reordering that resolved the player before the tournament
        // would still be reported here as the tournament being missing.
        long playerId = createPlayer("test_player_username", "test_player_email@test.com");

        postRegistration(Long.MAX_VALUE, playerId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    @Test
    @DisplayName("should return 404 Not Found when registering a player that does not exist")
    void createTournamentRegistrationPlayerNotFound() throws Exception {
        // Opened first: the status check runs before the player lookup, so a DRAFT would answer 409.
        long tournamentId = createTournamentWithNRegistrations("test_tournament_name", "test_board_game_name", 2, 0);

        postRegistration(tournamentId, Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    //-- Helpers

    private ResultActions postRegistration(long tournamentId, long playerId) throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments/{id}/registrations", tournamentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"playerId":%d}
                        """.formatted(playerId)));
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

    private ResultActions postTournament(Long boardGameId, String name, Integer maxPlayers) throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"boardGameId":"%d", "name":"%s", "maxPlayers":%d}
                        """.formatted(boardGameId, name, maxPlayers)));
    }

    private long createTournament(Long boardGameId, String name, Integer maxPlayers) throws Exception {
        MockHttpServletResponse response = postTournament(boardGameId, name, maxPlayers)
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

    private long createTournamentWithNRegistrations(String tournamentName, String boardGameName, Integer tournamentMaxPlayers, int numRegistrations) throws Exception {
        long boardGameId = createBoardGame(boardGameName, 1, 1 + numRegistrations);
        long tournamentId = createTournament(boardGameId, tournamentName, tournamentMaxPlayers);

        putTournament(tournamentId, boardGameId, tournamentName, tournamentMaxPlayers, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        for (int i = 0; i < numRegistrations; i++) {

            String username = "test_player_username_%d_for_%s".formatted(i + 1, tournamentName);
            String email = "test_player_email_%d_for_%s@test.com".formatted(i + 1, tournamentName);

            long playerId = createPlayer(username, email);

            mockMvc.perform(post("/api/v1/tournaments/{id}/registrations", tournamentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"playerId": %d}
                                    """.formatted(playerId)))
                    .andExpect(status().isCreated());
        }

        return tournamentId;
    }

    private ResultActions putTournament(long id, long boardGameId, String name, Integer tournamentMaxPlayers, TournamentStatus status) throws Exception {
        return mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"boardGameId":"%d", "name":"%s", "status":"%s", "maxPlayers":%d}
                        """.formatted(boardGameId, name, status, tournamentMaxPlayers)));
    }


    private ResultActions postBoardGame(String name, int minPlayers, int maxPlayers) throws Exception {
        return mockMvc.perform(post("/api/v1/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s", "minPlayers":"%d", "maxPlayers":"%d"}
                        """.formatted(name, minPlayers, maxPlayers)));
    }

    private long createBoardGame(String name, int minPlayers, int maxPlayers) throws Exception {
        MockHttpServletResponse response = postBoardGame(name, minPlayers, maxPlayers)
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

}