package io.arpicode.leagueapi.gamematch;

import com.jayway.jsonpath.JsonPath;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Game matches a test needs as setup, created through the API the way a client would.
 */
@TestComponent
public class GameMatchFixtures {

    private final MockMvc mockMvc;

    public GameMatchFixtures(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public long create(long tournamentId, OffsetDateTime scheduledAt, long... playerIds) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/gamematches")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tournamentId":%d, "scheduledAt":"%s", "playerIds":%s}
                    """.formatted(tournamentId, scheduledAt, Arrays.toString(playerIds))))
            .andExpect(status().isCreated())
            .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }

}
