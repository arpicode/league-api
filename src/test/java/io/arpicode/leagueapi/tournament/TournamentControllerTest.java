package io.arpicode.leagueapi.tournament;

import com.jayway.jsonpath.JsonPath;
import io.arpicode.leagueapi.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ApiIntegrationTest
@Transactional
class TournamentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void createTournament() throws Exception {
        Long boardGameId = createBoardGame();

        postTournament(boardGameId,
                "test_tournament_name",
                16,
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 1))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.boardGameId").value(boardGameId))
                .andExpect(jsonPath("$.name").value("test_tournament_name"))
                .andExpect(jsonPath("$.status").value(TournamentStatus.DRAFT.name()))
                .andExpect(jsonPath("$.maxPlayers").value(16))
                .andExpect(jsonPath("$.startsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.endsOn").value("2000-01-01"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    // -- Helpers

    private ResultActions postTournament(Long boardGameId, String name, int maxPlayers, LocalDate startsOn, LocalDate endsOn) throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"boardGameId":"%d", "name":"%s", "maxPlayers":"%d", "startsOn":"%s", "endsOn":"%s"}
                        """.formatted(boardGameId, name, maxPlayers, startsOn, endsOn)));
    }

    private long createBoardGame() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/boardgames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"test_board_game_name", "minPlayers":"1", "maxPlayers":"4"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        return ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
    }


}