package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.boardgame.BoardGame;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tournament", schema = "league")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY plus open-in-view=false: the association is resolved inside the service transaction
    // that maps to a DTO, never during serialization.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_game_id", nullable = false)
    private BoardGame boardGame;

    @NonNull
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "name_normalized", nullable = false, insertable = false, updatable = false)
    private String nameNormalized;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TournamentStatus status = TournamentStatus.DRAFT;

    @Column(name = "max_players")
    private Short maxPlayers;

    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Tournament(@NonNull BoardGame boardGame, @NonNull String name) {
        this.boardGame = boardGame;
        this.name = name;
    }

    public void transitionTo(TournamentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(
                    ErrorCode.TOURNAMENT_ILLEGAL_TRANSITION,
                    UserMessages.TOURNAMENT_ILLEGAL_TRANSITION.formatted(status, target));
        }
        this.status = target;
    }

    // A tournament stays deletable only while it is a DRAFT. Registrations attach from OPEN and
    // matches from IN_PROGRESS, so from then on the row records something the league actually did
    // and CANCELLED is the way out, not deletion.
    public void assertDeletable() {
        if (status != TournamentStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.TOURNAMENT_NOT_DELETABLE,
                    UserMessages.TOURNAMENT_NOT_DELETABLE.formatted(status));
        }
    }

    // OPEN is the only status that takes registrations. The check only holds until the insert if
    // the caller loaded this row under a lock, as TournamentRegistrationService does: otherwise a
    // concurrent transition can move the tournament past OPEN in between.
    public void assertOpenForRegistration() {
        if (this.status != TournamentStatus.OPEN) {
            throw new BusinessException(
                    ErrorCode.TOURNAMENT_NOT_OPEN,
                    UserMessages.TOURNAMENT_NOT_OPEN.formatted(this.getStatus())
            );
        }
    }

    // The roster is fixed once the tournament leaves OPEN, so withdrawing is only allowed while it
    // is. Same locking caveat as assertOpenForRegistration().
    public void assertOpenForWithdrawal() {
        if (this.status != TournamentStatus.OPEN) {
            throw new BusinessException(
                    ErrorCode.TOURNAMENT_NOT_OPEN,
                    UserMessages.TOURNAMENT_NOT_OPEN_FOR_WITHDRAWAL.formatted(this.getStatus())
            );
        }
    }

    // Lowering the limit below the players already confirmed is refused rather than demoting any of
    // them. Confirmed therefore never exceeds maxPlayers, so a withdrawal always frees a real place.
    public void assertMaxPlayersNotBelow(long confirmedPlayers) {
        if (maxPlayers != null && maxPlayers < confirmedPlayers) {
            throw new BusinessException(
                    ErrorCode.TOURNAMENT_MAX_PLAYERS_BELOW_CONFIRMED,
                    UserMessages.TOURNAMENT_MAX_PLAYERS_BELOW_CONFIRMED.formatted(confirmedPlayers)
            );
        }
    }

    // A tournament that has been played out or called off is a historical record: what the
    // league actually ran cannot be rewritten afterwards, so every editable field is frozen
    // together once the status is terminal. Re-sending the current values is not a change, so
    // it passes at any status and keeps a full-replace update idempotent -- the same rule
    // changeBoardGame() follows.
    public void replaceDetails(@NonNull String name, Short maxPlayers, LocalDate startsOn, LocalDate endsOn) {
        if (name.equals(this.name)
                && Objects.equals(maxPlayers, this.maxPlayers)
                && Objects.equals(startsOn, this.startsOn)
                && Objects.equals(endsOn, this.endsOn)) {
            return;
        }
        if (status.isTerminal()) {
            throw new BusinessException(
                    ErrorCode.TOURNAMENT_LOCKED,
                    UserMessages.TOURNAMENT_LOCKED.formatted(status));
        }
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }

    // Repointing a tournament at another game is only safe before it opens: matches get their game
    // through their tournament, so once they exist a change would silently rewrite the game they
    // were played with, and nothing in the database refuses it. Re-sending the current value is not
    // a change, so it passes at any status and keeps a full-replace update idempotent.
    public void changeBoardGame(@NonNull BoardGame target) {
        if (target.getId().equals(this.boardGame.getId())) {
            return;
        }
        if (status != TournamentStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.TOURNAMENT_BOARD_GAME_LOCKED,
                    UserMessages.TOURNAMENT_BOARD_GAME_LOCKED.formatted(status));
        }
        this.boardGame = target;
    }

}
