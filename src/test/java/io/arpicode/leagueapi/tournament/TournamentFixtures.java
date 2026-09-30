package io.arpicode.leagueapi.tournament;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.arpicode.leagueapi.boardgame.BoardGameFixtures;
import io.arpicode.leagueapi.player.PlayerFixtures;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tournaments a test needs as setup, created and moved through their statuses through the API the
 * way a client would, so a fixture can only produce a state a real client can reach.
 * <p>
 * The {@code create} overloads take explicit values, for tests that assert on them or clean up by
 * them. The {@code createDraft/Open/InProgress} methods pick everything the caller does not pass;
 * tournament names are unique in the schema, so the default name comes from a counter.
 */
@TestComponent
public class TournamentFixtures {

    // Room to spare, and deliberately not null: a tournament whose capacity is not what the test is
    // about still takes the ordinary capacity path, so only a test that asks for createOpen(null)
    // depends on the no-limit rule.
    private static final int DEFAULT_MAX_PLAYERS = 16;

    private final MockMvc mockMvc;
    private final BoardGameFixtures boardGames;
    private final PlayerFixtures players;
    private final AtomicInteger sequence = new AtomicInteger();

    public TournamentFixtures(MockMvc mockMvc, BoardGameFixtures boardGames, PlayerFixtures players) {
        this.mockMvc = mockMvc;
        this.boardGames = boardGames;
        this.players = players;
    }

    public long create(long boardGameId, String name) throws Exception {
        return create(boardGameId, name, null, null, null);
    }

    public long create(long boardGameId, String name, Integer maxPlayers, LocalDate startsOn, LocalDate endsOn) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"%s", "maxPlayers":%d, "startsOn":%s, "endsOn":%s}
                                """.formatted(boardGameId, name, maxPlayers, quotedOrNull(startsOn), quotedOrNull(endsOn))))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

    public long createDraft() throws Exception {
        return createThrough(DEFAULT_MAX_PLAYERS);
    }

    public long createOpen() throws Exception {
        return createOpen(DEFAULT_MAX_PLAYERS);
    }

    public long createOpen(Integer maxPlayers) throws Exception {
        return createThrough(maxPlayers, TournamentStatus.OPEN);
    }

    public long createInProgress() throws Exception {
        return createThrough(DEFAULT_MAX_PLAYERS, TournamentStatus.OPEN, TournamentStatus.IN_PROGRESS);
    }

    // Fills a tournament through the registration endpoint, one new player per registration.
    public void registerNewPlayers(long tournamentId, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            mockMvc.perform(post("/api/v1/tournaments/{id}/registrations", tournamentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"playerId":%d}
                                    """.formatted(players.create())))
                    .andExpect(status().isCreated());
        }
    }

    // PUT replaces the whole tournament, so every other field is resent as the tournament has it
    // now. They are read back through GET rather than tracked here, so this works on any tournament.
    public void changeMaxPlayers(long id, Integer maxPlayers) throws Exception {
        DocumentContext current = JsonPath.parse(mockMvc.perform(get("/api/v1/tournaments/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"%s", "status":"%s", "maxPlayers":%d, "startsOn":%s, "endsOn":%s}
                                """.formatted(
                                ((Number) current.read("$.boardGame.id")).longValue(),
                                current.read("$.name"),
                                current.read("$.status"),
                                maxPlayers,
                                quotedOrNull(current.read("$.startsOn")),
                                quotedOrNull(current.read("$.endsOn")))))
                .andExpect(status().isOk());
    }

    // Walks the state machine one legal step at a time, as a client has to: there is no way to
    // create a tournament directly in a later status.
    private long createThrough(Integer maxPlayers, TournamentStatus... path) throws Exception {
        long boardGameId = boardGames.create();
        String name = "fixture_tournament_" + sequence.incrementAndGet();
        long id = create(boardGameId, name, maxPlayers, null, null);

        for (TournamentStatus status : path) {
            transition(id, boardGameId, name, maxPlayers, status);
        }

        return id;
    }

    // PUT replaces the whole tournament, so a transition resends every field the tournament was
    // created with. Leaving one out clears it: a dropped maxPlayers would silently turn a capacity
    // test's tournament into an unlimited one.
    private void transition(long id, long boardGameId, String name, Integer maxPlayers, TournamentStatus status) throws Exception {
        mockMvc.perform(put("/api/v1/tournaments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"boardGameId":%d, "name":"%s", "status":"%s", "maxPlayers":%d}
                                """.formatted(boardGameId, name, status, maxPlayers)))
                .andExpect(status().isOk());
    }

    // A LocalDate from the caller, or the ISO string GET returns.
    private static String quotedOrNull(Object date) {
        return date == null ? "null" : "\"" + date + "\"";
    }

}
