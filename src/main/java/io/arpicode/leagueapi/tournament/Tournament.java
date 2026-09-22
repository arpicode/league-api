package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.boardgame.BoardGame;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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

    @Setter
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

    @Setter
    @Column(name = "max_players")
    private Short maxPlayers;

    @Setter
    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Setter
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

    // Repointing a tournament at another game is only safe before it opens: once matches exist
    // they reference the (tournament, board_game) pair, and the game they were played with
    // cannot be rewritten underneath them. Re-sending the current value is not a change, so it
    // passes at any status and keeps a full-replace update idempotent.
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
