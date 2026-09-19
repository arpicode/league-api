package io.arpicode.leagueapi.boardgame;

import io.arpicode.leagueapi.boardgame.dto.BoardGameRequest;
import io.arpicode.leagueapi.boardgame.dto.BoardGameResponse;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardGameService {

    private final BoardGameRepository boardGameRepository;

    public BoardGameService(BoardGameRepository boardGameRepository) {
        this.boardGameRepository = boardGameRepository;
    }

    @Transactional
    public BoardGameResponse create(BoardGameRequest boardGameRequest) {
        BoardGame boardGame = new BoardGame(boardGameRequest.name(), boardGameRequest.minPlayers(), boardGameRequest.maxPlayers());
        boardGame.setAvgDurationMin(boardGameRequest.avgDurationMin());

        BoardGame savedBoardGame = boardGameRepository.save(boardGame);

        return toBoardGameResponse(savedBoardGame);
    }

    @Transactional(readOnly = true)
    public Page<BoardGameResponse> list(Pageable pageable) {
        return boardGameRepository.findAll(pageable)
                .map(this::toBoardGameResponse);
    }

    @Transactional(readOnly = true)
    public BoardGameResponse getById(long id) {
        BoardGame boardGame = boardGameRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BOARD_GAME_NOT_FOUND,
                        UserMessages.BOARD_GAME_NOT_FOUND.formatted(id)));

        return toBoardGameResponse(boardGame);
    }

    @Transactional
    public BoardGameResponse update(long id, BoardGameRequest boardGameRequest) {
        BoardGame boardGame = boardGameRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BOARD_GAME_NOT_FOUND,
                        UserMessages.BOARD_GAME_NOT_FOUND.formatted(id)));

        boardGame.setName(boardGameRequest.name());
        boardGame.setMinPlayers(boardGameRequest.minPlayers());
        boardGame.setMaxPlayers(boardGameRequest.maxPlayers());
        boardGame.setAvgDurationMin(boardGameRequest.avgDurationMin());

        boardGameRepository.saveAndFlush(boardGame);

        return toBoardGameResponse(boardGame);
    }

    @Transactional
    public void delete(long id) {
        BoardGame boardGame = boardGameRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BOARD_GAME_NOT_FOUND,
                        UserMessages.BOARD_GAME_NOT_FOUND.formatted(id)));

        boardGameRepository.delete(boardGame);
    }

    private BoardGameResponse toBoardGameResponse(BoardGame boardGame) {
        return new BoardGameResponse(
                boardGame.getId(),
                boardGame.getName(),
                boardGame.getMinPlayers(),
                boardGame.getMaxPlayers(),
                boardGame.getAvgDurationMin(),
                boardGame.getCreatedAt(),
                boardGame.getUpdatedAt()
        );
    }
}
