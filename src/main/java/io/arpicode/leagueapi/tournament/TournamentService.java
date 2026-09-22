package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.boardgame.BoardGameRepository;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.dto.TournamentCreateRequest;
import io.arpicode.leagueapi.tournament.dto.TournamentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final BoardGameRepository boardGameRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             BoardGameRepository boardGameRepository) {
        this.tournamentRepository = tournamentRepository;
        this.boardGameRepository = boardGameRepository;
    }

    @Transactional
    public TournamentResponse create(TournamentCreateRequest tournamentCreateRequest) {
        if (!boardGameRepository.existsById(tournamentCreateRequest.boardGameId())) {
            throw new BusinessException(
                    ErrorCode.BOARD_GAME_NOT_FOUND,
                    UserMessages.BOARD_GAME_NOT_FOUND.formatted(tournamentCreateRequest.boardGameId()));
        }

        Tournament tournament = new Tournament(tournamentCreateRequest.boardGameId(), tournamentCreateRequest.name());
        tournament.setMaxPlayers(tournamentCreateRequest.maxPlayers());
        tournament.setStartsOn(tournamentCreateRequest.startsOn());
        tournament.setEndsOn(tournamentCreateRequest.endsOn());

        Tournament saved = tournamentRepository.save(tournament);

        return toTournamentResponse(saved);
    }

    private static TournamentResponse toTournamentResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getBoardGameId(),
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
