package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.boardgame.BoardGame;
import io.arpicode.leagueapi.boardgame.BoardGameRepository;
import io.arpicode.leagueapi.boardgame.dto.BoardGameSummary;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.dto.TournamentCreateRequest;
import io.arpicode.leagueapi.tournament.dto.TournamentResponse;
import io.arpicode.leagueapi.tournament.dto.TournamentUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final BoardGameRepository boardGameRepository;

    public TournamentService(TournamentRepository tournamentRepository, BoardGameRepository boardGameRepository) {
        this.tournamentRepository = tournamentRepository;
        this.boardGameRepository = boardGameRepository;
    }

    @Transactional
    public TournamentResponse create(TournamentCreateRequest tournamentCreateRequest) {
        // Loaded rather than referenced: getReferenceById would hand back a proxy without a query,
        // so a bad id would only surface as a raw fk_tournament_board_game violation.
        BoardGame boardGame = findBoardGame(tournamentCreateRequest.boardGameId());

        Tournament tournament = new Tournament(boardGame, tournamentCreateRequest.name());
        tournament.setMaxPlayers(tournamentCreateRequest.maxPlayers());
        tournament.setStartsOn(tournamentCreateRequest.startsOn());
        tournament.setEndsOn(tournamentCreateRequest.endsOn());

        Tournament saved = tournamentRepository.save(tournament);

        return toTournamentResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> list(Pageable pageable) {
        return tournamentRepository.findAll(pageable)
                .map(this::toTournamentResponse);
    }

    @Transactional(readOnly = true)
    public TournamentResponse getById(long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TOURNAMENT_NOT_FOUND,
                        UserMessages.TOURNAMENT_NOT_FOUND.formatted(id)));

        return toTournamentResponse(tournament);
    }

    @Transactional
    public TournamentResponse update(long id, TournamentUpdateRequest tournamentUpdateRequest) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TOURNAMENT_NOT_FOUND,
                        UserMessages.TOURNAMENT_NOT_FOUND.formatted(id)));
        BoardGame boardGame = findBoardGame(tournamentUpdateRequest.boardGameId());

        tournament.changeBoardGame(boardGame);
        tournament.setName(tournamentUpdateRequest.name());
        tournament.transitionTo(tournamentUpdateRequest.status());
        tournament.setMaxPlayers(tournamentUpdateRequest.maxPlayers());
        tournament.setStartsOn(tournamentUpdateRequest.startsOn());
        tournament.setEndsOn(tournamentUpdateRequest.endsOn());

        tournamentRepository.saveAndFlush(tournament);

        return toTournamentResponse(tournament);
    }


    private BoardGame findBoardGame(long id) {
        return boardGameRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BOARD_GAME_NOT_FOUND,
                        UserMessages.BOARD_GAME_NOT_FOUND.formatted(id)));
    }

    private TournamentResponse toTournamentResponse(Tournament tournament) {
        BoardGame boardGame = tournament.getBoardGame();

        return new TournamentResponse(
                tournament.getId(),
                new BoardGameSummary(boardGame.getId(), boardGame.getName()),
                tournament.getName(),
                tournament.getStatus(),
                tournament.getMaxPlayers(),
                tournament.getStartsOn(),
                tournament.getEndsOn(),
                tournament.getCreatedAt(),
                tournament.getUpdatedAt()
        );
    }

}
