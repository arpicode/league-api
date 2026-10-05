package io.arpicode.leagueapi.tournament.match;

import io.arpicode.leagueapi.tournament.match.dto.GameMatchCreateRequest;
import io.arpicode.leagueapi.tournament.match.dto.GameMatchResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/tournaments")
public class GameMatchController {

    private final GameMatchService gameMatchService;

    public GameMatchController(GameMatchService gameMatchService) {
        this.gameMatchService = gameMatchService;
    }

    @PostMapping("/{id}/matches")
    public ResponseEntity<GameMatchResponse> create(
        @PathVariable long id,
        @Valid @RequestBody GameMatchCreateRequest gameMatchCreateRequest) {
        GameMatchResponse saved = gameMatchService.create(id, gameMatchCreateRequest);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .replaceQuery(null)
            .path("/{matchId}")
            .buildAndExpand(saved.id())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(saved);
    }

    @GetMapping("/{id}/matches/{matchId}")
    public GameMatchResponse getById(@PathVariable long id, @PathVariable long matchId) {
        return gameMatchService.getById(id, matchId);
    }

}
