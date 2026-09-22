package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.tournament.dto.TournamentCreateRequest;
import io.arpicode.leagueapi.tournament.dto.TournamentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@Valid @RequestBody TournamentCreateRequest tournamentCreateRequest) {
        TournamentResponse saved = tournamentService.create(tournamentCreateRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .replaceQuery(null)
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(saved);
    }

}
