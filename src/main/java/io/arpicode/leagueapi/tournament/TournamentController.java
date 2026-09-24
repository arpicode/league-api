package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.tournament.dto.TournamentCreateRequest;
import io.arpicode.leagueapi.tournament.dto.TournamentResponse;
import io.arpicode.leagueapi.tournament.dto.TournamentUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping
    public PagedModel<TournamentResponse> list(@PageableDefault(sort = "nameNormalized") Pageable pageable) {
        return new PagedModel<>(tournamentService.list(pageable));
    }

    @GetMapping("/{id}")
    public TournamentResponse getById(@PathVariable long id) {
        return tournamentService.getById(id);
    }

    @PutMapping("/{id}")
    public TournamentResponse update(@PathVariable long id, @Valid @RequestBody TournamentUpdateRequest tournamentUpdateRequest) {
        return tournamentService.update(id, tournamentUpdateRequest);
    }

}
