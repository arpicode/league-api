package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.ApiIntegrationTest;
import io.arpicode.leagueapi.player.PlayerFixtures;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ApiIntegrationTest
@Transactional
class TournamentRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerFixtures players;

    @Autowired
    private TournamentFixtures tournaments;

    // -- Create

    @Test
    @DisplayName("should create a tournament confirmed registration when there's enough room left")
    void createTournamentConfirmedRegistration() throws Exception {
        long playerId = players.create("test_player_username", "test_player_email@test.com");
        long tournamentId = tournaments.createOpen(16);

        postRegistration(tournamentId, playerId)
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
        long playerId = players.create();
        long tournamentAId = tournaments.createOpen(2);
        long tournamentBId = tournaments.createOpen(2);
        registerNewPlayers(tournamentBId, 2);

        postRegistration(tournamentAId, playerId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(TournamentRegistrationStatus.CONFIRMED.name()));
    }

    @Test
    @DisplayName("should create a waitlisted tournament registration when there's not enough room left")
    void createTournamentRegistrationWhenNotEnoughRoom() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createOpen(2);
        registerNewPlayers(tournamentId, 2);

        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(TournamentRegistrationStatus.WAITLISTED.name()))
                .andExpect(jsonPath("$.waitlistPosition").value(1));
    }

    @Test
    @DisplayName("should create a confirmed registration when the tournament has no maximum number of players")
    void createTournamentRegistrationWithoutMaxPlayers() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createOpen(null);

        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(TournamentRegistrationStatus.CONFIRMED.name()));
    }

    @Test
    @DisplayName("should return 409 Conflict when the player is already registered for the tournament")
    void createTournamentRegistrationDuplicate() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createOpen();

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
        long playerId = players.create();
        long tournamentId = tournaments.createDraft();

        postRegistration(tournamentId, playerId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_OPEN.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_OPEN.formatted(TournamentStatus.DRAFT)));
    }

    @Test
    @DisplayName("should return 409 Conflict when registering for a tournament that is already in progress")
    void createTournamentRegistrationInProgressTournament() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createInProgress();

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
        long playerId = players.create();

        postRegistration(Long.MAX_VALUE, playerId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    @Test
    @DisplayName("should return 404 Not Found when registering a player that does not exist")
    void createTournamentRegistrationPlayerNotFound() throws Exception {
        // Opened first: the status check runs before the player lookup, so a DRAFT would answer 409.
        long tournamentId = tournaments.createOpen();

        postRegistration(tournamentId, Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PLAYER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.PLAYER_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    // -- Helpers

    private ResultActions postRegistration(long tournamentId, long playerId) throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments/{id}/registrations", tournamentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"playerId":%d}
                        """.formatted(playerId)));
    }

    // Fills a tournament through the endpoint under test, one new player per registration. It stays
    // here rather than in TournamentFixtures while no other feature's tests need registrations.
    private void registerNewPlayers(long tournamentId, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            postRegistration(tournamentId, players.create())
                    .andExpect(status().isCreated());
        }
    }

}
