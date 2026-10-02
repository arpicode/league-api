package io.arpicode.leagueapi.boardgame;

import com.jayway.jsonpath.JsonPath;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Board games a test needs as setup, created through the API the way a client would.
 * <p>
 * Pass explicit values when the test asserts on them or its cleanup deletes by them; otherwise
 * let the shorter overloads pick them. Names are unique in the schema, so the default name comes
 * from a counter: deterministic, and never reused within a run.
 */
@TestComponent
public class BoardGameFixtures {

    private final MockMvc mockMvc;
    private final AtomicInteger sequence = new AtomicInteger();

    public BoardGameFixtures(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public long create() throws Exception {
        return create("fixture_board_game_" + sequence.incrementAndGet());
    }

    public long create(String name) throws Exception {
        return create(name, 1, 4);
    }

    public long create(String name, int minPlayers, int maxPlayers) throws Exception {
        return create(name, minPlayers, maxPlayers, null);
    }

    public long create(String name, int minPlayers, int maxPlayers, Integer avgDurationMin) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/boardgames")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s", "minPlayers":%d, "maxPlayers":%d, "avgDurationMin":%d}
                    """.formatted(name, minPlayers, maxPlayers, avgDurationMin)))
            .andExpect(status().isCreated())
            .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

}
