package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.player.Player;
import io.arpicode.leagueapi.player.PlayerRepository;
import io.arpicode.leagueapi.player.dto.PlayerSummary;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.dto.TournamentRegistrationCreateRequest;
import io.arpicode.leagueapi.tournament.dto.TournamentRegistrationResponse;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // Repeatable read runs both queries on one snapshot, so a registration read as WAITLISTED is still
    // on the waitlist when its position is computed, even if it is promoted or withdrawn in between.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TournamentRegistrationResponse getById(long tournamentId, long playerId) {
        TournamentRegistration tournamentRegistration = tournamentRegistrationRepository
            .findById(new TournamentPlayerId(tournamentId, playerId))
            .orElseThrow(() -> registrationNotFound(tournamentId, playerId));

        return toTournamentRegistrationResponse(
            tournamentRegistration,
            tournamentRegistration.getPlayer(),
            waitlistPosition(tournamentRegistration)
        );
    }

    // Repeatable read keeps the page, its count and the positions on one snapshot.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<TournamentRegistrationResponse> list(long tournamentId, Pageable pageable) {
        Page<TournamentRegistration> registrations =
            tournamentRegistrationRepository.findByTournamentId(tournamentId, pageable);

        // An empty page is either past the end of an existing roster or a missing tournament.
        if (registrations.isEmpty() && !tournamentRepository.existsById(tournamentId)) {
            throw new BusinessException(
                ErrorCode.TOURNAMENT_NOT_FOUND,
                UserMessages.TOURNAMENT_NOT_FOUND.formatted(tournamentId)
            );
        }

        Map<Long, Integer> positions = waitlistPositions(tournamentId, registrations.getContent());

        return registrations.map(registration -> toTournamentRegistrationResponse(
            registration,
            registration.getPlayer(),
            positions.get(registration.getId().getPlayerId())
        ));
    }

    // Takes the same lock as create, so withdrawals, registrations and promotions on one tournament
    // run one at a time: otherwise a registration can waitlist itself for a place a concurrent
    // withdrawal is freeing, or two withdrawals can both promote the same waitlisted row. The lock
    // comes before the registration is loaded, so its status is read after any such race settles.
    @Transactional
    public void delete(long tournamentId, long playerId) {
        Tournament tournament = findTournamentForUpdate(tournamentId);

        tournament.assertOpenForWithdrawal();

        // The lock just proved the tournament exists, so a miss here can only be the registration.
        TournamentRegistration tournamentRegistration = tournamentRegistrationRepository
            .findById(new TournamentPlayerId(tournamentId, playerId))
            .orElseThrow(() -> new BusinessException(
                ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND,
                UserMessages.TOURNAMENT_REGISTRATION_NOT_FOUND.formatted(playerId, tournamentId)
            ));

        boolean freesAPlace = tournamentRegistration.isConfirmed();

        tournamentRegistrationRepository.delete(tournamentRegistration);

        if (freesAPlace) {
            promoteFromWaitlist(tournamentId, Limit.of(1));
        }
    }

    // Only for a tournament leaving OPEN. The caller must hold the tournament lock, as
    // TournamentService.update does, or a registration could waitlist itself after the delete.
    public void deleteAllWaitlisted(long tournamentId) {
        List<TournamentRegistration> waitlisted = tournamentRegistrationRepository.findByTournamentIdAndStatus(tournamentId, TournamentRegistrationStatus.WAITLISTED);
        tournamentRegistrationRepository.deleteAll(waitlisted);
    }

    // Fits the roster to a tournament's new maxPlayers. The caller must hold the tournament lock, as
    // TournamentService.update does, or a registration could take a place between the count and the
    // promotions. Raising the limit, or removing it, hands the new places to the waitlist, but only
    // while OPEN: the roster is fixed from IN_PROGRESS.
    public void applyMaxPlayersChange(Tournament tournament) {
        long confirmedCount = tournamentRegistrationRepository
            .countByStatusAndTournamentId(TournamentRegistrationStatus.CONFIRMED, tournament.getId());

        tournament.assertMaxPlayersNotBelow(confirmedCount);

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            return;
        }

        Short maxPlayers = tournament.getMaxPlayers();
        if (maxPlayers == null) {
            promoteFromWaitlist(tournament.getId(), Limit.unlimited());
        } else if (confirmedCount < maxPlayers) {
            promoteFromWaitlist(tournament.getId(), Limit.of(Math.toIntExact(maxPlayers - confirmedCount)));
        }
    }

    private void promoteFromWaitlist(long tournamentId, Limit places) {
        tournamentRegistrationRepository
            .findByTournamentIdAndStatusOrderByRegisteredAtAscIdPlayerIdAsc(
                tournamentId, TournamentRegistrationStatus.WAITLISTED, places)
            .forEach(TournamentRegistration::promote);
    }

    // The tournament is the parent resource in the URL, so a missing one is reported as such rather
    // than as a missing registration. It is only looked up on this path, sparing the happy path a query.
    private BusinessException registrationNotFound(long tournamentId, long playerId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            return new BusinessException(
                ErrorCode.TOURNAMENT_NOT_FOUND,
                UserMessages.TOURNAMENT_NOT_FOUND.formatted(tournamentId)
            );
        }
        return new BusinessException(
            ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND,
            UserMessages.TOURNAMENT_REGISTRATION_NOT_FOUND.formatted(playerId, tournamentId)
        );
    }

    private Integer waitlistPosition(TournamentRegistration tournamentRegistration) {
        if (tournamentRegistration.isConfirmed()) {
            return null;
        }

        TournamentPlayerId id = tournamentRegistration.getId();
        long position = tournamentRegistrationRepository
            .findWaitlistPosition(id.getTournamentId(), id.getPlayerId())
            .orElseThrow(() -> new IllegalStateException(
                "Registration (tournament %d, player %d) is WAITLISTED but has no waitlist position"
                    .formatted(id.getTournamentId(), id.getPlayerId())));
        return Math.toIntExact(position);
    }

    private Map<Long, Integer> waitlistPositions(long tournamentId, List<TournamentRegistration> registrations) {
        List<Long> waitlistedPlayerIds = registrations.stream()
            .filter(registration -> !registration.isConfirmed())
            .map(registration -> registration.getId().getPlayerId())
            .toList();

        if (waitlistedPlayerIds.isEmpty()) {
            return Map.of();
        }

        return tournamentRegistrationRepository.findWaitlistPositions(tournamentId, waitlistedPlayerIds).stream()
            .collect(Collectors.toMap(
                TournamentRegistrationRepository.WaitlistPosition::getPlayerId,
                position -> Math.toIntExact(position.getPosition())
            ));
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
