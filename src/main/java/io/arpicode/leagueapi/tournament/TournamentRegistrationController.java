package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.tournament.dto.TournamentRegistrationCreateRequest;
import io.arpicode.leagueapi.tournament.dto.TournamentRegistrationResponse;
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

    // V009's order by default: registered_at alone can tie, and a sort without a unique last key
    // lets rows repeat or go missing between pages.
    @GetMapping("/{id}/registrations")
    public PagedModel<TournamentRegistrationResponse> list(
            @PathVariable long id,
            @PageableDefault(sort = {"registeredAt", "id.playerId"}) Pageable pageable) {
        return new PagedModel<>(tournamentRegistrationService.list(id, pageable));
    }

    @GetMapping("/{id}/registrations/{playerId}")
    public TournamentRegistrationResponse getById(@PathVariable long id, @PathVariable long playerId) {
        return tournamentRegistrationService.getById(id, playerId);
    }
}
