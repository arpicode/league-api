package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.ApiIntegrationTest;
import io.arpicode.leagueapi.CommitsData;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    // -- Read

    @Test
    @DisplayName("should return a confirmed registration without a waitlist position")
    void getTournamentConfirmedRegistration() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createOpen();

        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated());

        getRegistration(tournamentId, playerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistPosition").value(nullValue()));
    }

    @Test
    @DisplayName("should return each waitlisted registration's position in the tournament's waitlist")
    void getTournamentWaitlistedRegistrationPositions() throws Exception {
        // All registrations share registered_at inside this rolled-back transaction, so the player id
        // decides the order here: players created first rank first.
        long tournamentId = tournaments.createOpen(2);
        registerNewPlayers(tournamentId, 2);

        long firstPlayerId = players.create();
        long secondPlayerId = players.create();
        postRegistration(tournamentId, firstPlayerId)
                .andExpect(status().isCreated());
        postRegistration(tournamentId, secondPlayerId)
                .andExpect(status().isCreated());

        getRegistration(tournamentId, firstPlayerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistPosition").value(1));
        getRegistration(tournamentId, secondPlayerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistPosition").value(2));
    }

    @Test
    @DisplayName("should not count another tournament's waitlist in a waitlisted registration's position")
    void getTournamentWaitlistedRegistrationForTargetedTournament() throws Exception {
        // Its waitlisted player is created first, so it would rank ahead if the tournament were ignored.
        long otherTournamentId = tournaments.createOpen(2);
        registerNewPlayers(otherTournamentId, 3);

        long tournamentId = tournaments.createOpen(2);
        registerNewPlayers(tournamentId, 2);

        long playerId = players.create();
        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated());

        getRegistration(tournamentId, playerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistPosition").value(1));
    }

    @Test
    @DisplayName("should rank the waitlist by registration time before player id")
    @CommitsData
    void getTournamentWaitlistedRegistrationByRegistrationTime() throws Exception {
        // Each registration commits in its own transaction, like real requests do, since Postgres
        // now() is frozen for the life of a transaction and would give both the same registered_at.
        long tournamentId = tournaments.createOpen(2);
        registerNewPlayers(tournamentId, 2);

        long lowerPlayerId = players.create();
        long higherPlayerId = players.create();
        postRegistration(tournamentId, higherPlayerId)
                .andExpect(status().isCreated());
        postRegistration(tournamentId, lowerPlayerId)
                .andExpect(status().isCreated());

        getRegistration(tournamentId, lowerPlayerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistPosition").value(2));
    }

    @Test
    @DisplayName("should return 404 Not Found when the player is not registered for the tournament")
    void getTournamentRegistrationNotRegistered() throws Exception {
        long playerId = players.create();
        long tournamentId = tournaments.createOpen();

        getRegistration(tournamentId, playerId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_REGISTRATION_NOT_FOUND.formatted(playerId, tournamentId)));
    }

    @Test
    @DisplayName("should return 404 Not Found when getting a registration for a tournament that does not exist")
    void getTournamentRegistrationTournamentNotFound() throws Exception {
        // A real player id, so the only thing missing is the tournament.
        long playerId = players.create();

        getRegistration(Long.MAX_VALUE, playerId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    // -- List

    @Test
    @DisplayName("should list only the targeted tournament's registrations and rank only its waitlist")
    void listTournamentRegistrationsForTargetedTournament() throws Exception {
        // Its waitlisted player is created first, so it would rank ahead if the tournament were ignored.
        long otherTournamentId = tournaments.createOpen(2);
        registerNewPlayers(otherTournamentId, 3);

        long tournamentId = tournaments.createOpen(2);
        registerNewPlayers(tournamentId, 2);

        long playerId = players.create();
        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated());

        listRegistrations(tournamentId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[2].player.id").value(playerId))
                .andExpect(jsonPath("$.content[2].waitlistPosition").value(1));
    }

    @Test
    @DisplayName("should rank a waitlisted registration across the whole waitlist, not within its page")
    void listTournamentRegistrationsBeyondFirstPage() throws Exception {
        // Two confirmed and two waitlisted: with pages of three, the second waitlisted registration
        // is alone on the second page, where its rank within the page would be 1.
        long tournamentId = tournaments.createOpen(2);
        registerNewPlayers(tournamentId, 3);

        long playerId = players.create();
        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated());

        listRegistrations(tournamentId, "page=1&size=3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].player.id").value(playerId))
                .andExpect(jsonPath("$.content[0].waitlistPosition").value(2));
    }

    @Test
    @DisplayName("should keep waitlist positions in registration order whatever sort the client requests")
    void listTournamentRegistrationsSortedByClient() throws Exception {
        long tournamentId = tournaments.createOpen(2);
        registerNewPlayers(tournamentId, 3);

        long playerId = players.create();
        postRegistration(tournamentId, playerId)
                .andExpect(status().isCreated());

        // Newest player first: the last waitlisted registration heads the only page, and a position
        // derived from where rows fall in the response would no longer be 2.
        listRegistrations(tournamentId, "sort=id.playerId,desc&size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].player.id").value(playerId))
                .andExpect(jsonPath("$.content[0].waitlistPosition").value(2));
    }

    @Test
    @DisplayName("should list the roster in registration order and rank its waitlist by registration time")
    @CommitsData
    void listTournamentRegistrationsByRegistrationTime() throws Exception {
        // Each registration commits in its own transaction, like real requests do, since Postgres
        // now() is frozen for the life of a transaction and would give both the same registered_at.
        long tournamentId = tournaments.createOpen(2);
        registerNewPlayers(tournamentId, 2);

        long lowerPlayerId = players.create();
        long higherPlayerId = players.create();
        postRegistration(tournamentId, higherPlayerId)
                .andExpect(status().isCreated());
        postRegistration(tournamentId, lowerPlayerId)
                .andExpect(status().isCreated());

        listRegistrations(tournamentId, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[3].player.id").value(lowerPlayerId))
                .andExpect(jsonPath("$.content[3].waitlistPosition").value(2));
    }

    @Test
    @DisplayName("should return an empty roster for a tournament nobody has registered for yet")
    void listTournamentRegistrationsWithoutRegistrations() throws Exception {
        long tournamentId = tournaments.createOpen();

        listRegistrations(tournamentId, "")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 404 Not Found when listing the registrations of a tournament that does not exist")
    void listTournamentRegistrationsTournamentNotFound() throws Exception {
        listRegistrations(Long.MAX_VALUE, "")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOURNAMENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(UserMessages.TOURNAMENT_NOT_FOUND.formatted(Long.MAX_VALUE)));
    }

    // -- Helpers

    private ResultActions postRegistration(long tournamentId, long playerId) throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments/{id}/registrations", tournamentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"playerId":%d}
                        """.formatted(playerId)));
    }

    private ResultActions getRegistration(long tournamentId, long playerId) throws Exception {
        return mockMvc.perform(get("/api/v1/tournaments/{id}/registrations/{playerId}", tournamentId, playerId));
    }

    // Paging and sorting go in the query string as a client sends them, e.g. "page=1&size=3".
    private ResultActions listRegistrations(long tournamentId, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/tournaments/{id}/registrations?" + query, tournamentId));
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
