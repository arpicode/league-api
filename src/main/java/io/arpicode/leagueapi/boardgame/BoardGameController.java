package io.arpicode.leagueapi.boardgame;

import io.arpicode.leagueapi.boardgame.dto.BoardGameRequest;
import io.arpicode.leagueapi.boardgame.dto.BoardGameResponse;
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
@RequestMapping("/api/v1/boardgames")
public class BoardGameController {

    private final BoardGameService boardGameService;

    public BoardGameController(BoardGameService boardGameService) {
        this.boardGameService = boardGameService;
    }

    @PostMapping
    public ResponseEntity<BoardGameResponse> create(@Valid @RequestBody BoardGameRequest boardGameRequest) {
        BoardGameResponse saved = boardGameService.create(boardGameRequest);

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
    public PagedModel<BoardGameResponse> list(@PageableDefault(sort = "name") Pageable pageable) {
        return new PagedModel<>(boardGameService.list(pageable));
    }

    @GetMapping("/{id}")
    public BoardGameResponse getById(@PathVariable long id) {
        return boardGameService.getById(id);
    }

    @PutMapping("/{id}")
    public BoardGameResponse update(@PathVariable long id, @Valid @RequestBody BoardGameRequest boardGameRequest) {
        return boardGameService.update(id, boardGameRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        boardGameService.delete(id);
    }

}
