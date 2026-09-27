package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.player.Player;
import io.arpicode.leagueapi.player.PlayerRepository;
import io.arpicode.leagueapi.player.dto.PlayerSummary;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.dto.TournamentRegistrationCreateRequest;
import io.arpicode.leagueapi.tournament.dto.TournamentRegistrationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentRegistrationService {

    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;

    public TournamentRegistrationService(
            TournamentRegistrationRepository tournamentRegistrationRepository,
            PlayerRepository playerRepository,
            TournamentRepository tournamentRepository
    ) {
        this.tournamentRegistrationRepository = tournamentRegistrationRepository;
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional
    public TournamentRegistrationResponse create(long tournamentId, TournamentRegistrationCreateRequest tournamentRegistrationCreateRequest) {
        Tournament tournament = findTournamentForUpdate(tournamentId);

        tournament.assertOpenForRegistration();

        Player player = findPlayer(tournamentRegistrationCreateRequest.playerId());
        TournamentPlayerId tournamentPlayerId = new TournamentPlayerId(tournamentId, player.getId());

        if (tournamentRegistrationRepository.existsById(tournamentPlayerId)) {
            throw new BusinessException(
                    ErrorCode.TOURNAMENT_REGISTRATION_ALREADY_EXISTS,
                    UserMessages.TOURNAMENT_REGISTRATION_PLAYER_ALREADY_REGISTERED.formatted(player.getId(), tournamentId)
            );
        }

        Short maxPlayersAllowed = tournament.getMaxPlayers();
        long registeredPlayersCount = tournamentRegistrationRepository.countByStatusAndTournamentId(TournamentRegistrationStatus.CONFIRMED, tournamentId);
        long waitlistedPlayersCount = tournamentRegistrationRepository.countByStatusAndTournamentId(TournamentRegistrationStatus.WAITLISTED, tournamentId);
        TournamentRegistration tournamentRegistration;
        Integer waitlistPosition;

        // If max players for tournament is null, any number of players can register
        if (maxPlayersAllowed == null || registeredPlayersCount < maxPlayersAllowed) {
            tournamentRegistration = new TournamentRegistration(tournament, player, TournamentRegistrationStatus.CONFIRMED);
            waitlistPosition = null;
        } else {
            tournamentRegistration = new TournamentRegistration(tournament, player, TournamentRegistrationStatus.WAITLISTED);
            waitlistPosition = Math.toIntExact(waitlistedPlayersCount + 1);
        }

        TournamentRegistration saved = tournamentRegistrationRepository.saveAndFlush(tournamentRegistration);

        return toTournamentRegistrationResponse(saved, player, waitlistPosition);
    }

    private TournamentRegistrationResponse toTournamentRegistrationResponse(TournamentRegistration tournamentRegistration, Player player, Integer waitlistPosition) {
        return new TournamentRegistrationResponse(
                tournamentRegistration.getId().getTournamentId(),
                new PlayerSummary(player.getId(), player.getUsername()),
                tournamentRegistration.getRegisteredAt(),
                tournamentRegistration.getStatus(),
                waitlistPosition
        );
    }

    private Player findPlayer(long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PLAYER_NOT_FOUND,
                        UserMessages.PLAYER_NOT_FOUND.formatted(id)
                ));
    }

    private Tournament findTournamentForUpdate(long id) {
        return tournamentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TOURNAMENT_NOT_FOUND,
                        UserMessages.TOURNAMENT_NOT_FOUND.formatted(id)
                ));
    }

}
