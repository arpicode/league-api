package io.arpicode.leagueapi.player;

import com.jayway.jsonpath.JsonPath;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Players a test needs as setup, created through the API the way a client would.
 * <p>
 * Pass explicit values when the test asserts on them or its cleanup deletes by them; otherwise
 * let {@link #create()} pick them. Usernames and emails are unique in the schema, so the defaults
 * come from a counter: deterministic, and never reused within a run.
 */
@TestComponent
public class PlayerFixtures {

    private final MockMvc mockMvc;
    private final AtomicInteger sequence = new AtomicInteger();

    public PlayerFixtures(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public long create() throws Exception {
        String username = "fixture_player_" + sequence.incrementAndGet();

        return create(username, username + "@example.com");
    }

    public long create(String username, String email) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s", "email":"%s"}
                                """.formatted(username, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

}
