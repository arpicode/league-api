package io.arpicode.leagueapi.player;

import io.arpicode.leagueapi.player.dto.PlayerRequest;
import io.arpicode.leagueapi.player.dto.PlayerResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping
    public ResponseEntity<PlayerResponse> create(@Valid @RequestBody PlayerRequest playerRequest) {
        PlayerResponse saved = playerService.create(playerRequest);

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

    // PagedModel rather than Page: Page's JSON shape is explicitly not a stable contract
    // in Spring Data, PagedModel's {content, page:{...}} envelope is.
    @GetMapping
    public PagedModel<PlayerResponse> list(@PageableDefault(sort = "id") Pageable pageable) {
        return new PagedModel<>(playerService.list(pageable));
    }

    @GetMapping("/{id}")
    public PlayerResponse getById(@PathVariable long id) {
        return playerService.getById(id);
    }

    @PutMapping("/{id}")
    public PlayerResponse update(@PathVariable long id, @Valid @RequestBody PlayerRequest playerRequest) {
        return playerService.update(id, playerRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        playerService.delete(id);
    }

}
