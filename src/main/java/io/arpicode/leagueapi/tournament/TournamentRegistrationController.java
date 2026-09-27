package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.tournament.dto.TournamentRegistrationCreateRequest;
import io.arpicode.leagueapi.tournament.dto.TournamentRegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentRegistrationController {

    private final TournamentRegistrationService tournamentRegistrationService;

    public TournamentRegistrationController(TournamentRegistrationService tournamentRegistrationService) {
        this.tournamentRegistrationService = tournamentRegistrationService;
    }

    @PostMapping("/{id}/registrations")
    public ResponseEntity<TournamentRegistrationResponse> create(
            @PathVariable long id,
            @Valid @RequestBody TournamentRegistrationCreateRequest tournamentRegistrationCreateRequest) {
        TournamentRegistrationResponse saved = tournamentRegistrationService.create(id, tournamentRegistrationCreateRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .replaceQuery(null)
                .path("/{id}")
                .buildAndExpand(saved.player().id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(saved);
    }
}
