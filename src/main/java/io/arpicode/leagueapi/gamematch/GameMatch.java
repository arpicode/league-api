package io.arpicode.leagueapi.gamematch;

import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.Tournament;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_match", schema = "league")
public class GameMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false, updatable = false)
    private Tournament tournament;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameMatchStatus status = GameMatchStatus.SCHEDULED;

    @Column(name = "round_number")
    private Short roundNumber;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public GameMatch(@NonNull Tournament tournament, Short roundNumber, OffsetDateTime scheduledAt) {
        tournament.assertInProgress();
        this.tournament = tournament;
        this.roundNumber = roundNumber;
        this.scheduledAt = scheduledAt;
    }

    // Each action sets the status together with its timestamp: V011's CHECK constraints pair them,
    // so a status changed on its own would only fail at flush, as a generic integrity violation.
    public void start(@NonNull OffsetDateTime startedAt) {
        transitionTo(GameMatchStatus.IN_PROGRESS);
        this.startedAt = startedAt;
    }

    // A completed match is final. A second completion is refused rather than ignored, so the
    // organizer whose result arrives after another one's learns it was not recorded, whether the
    // two requests overlapped (@Version) or not (this transition).
    public void complete(@NonNull OffsetDateTime completedAt) {
        transitionTo(GameMatchStatus.COMPLETED);
        this.completedAt = completedAt;
    }

    public void cancel() {
        transitionTo(GameMatchStatus.CANCELLED);
    }

    private void transitionTo(GameMatchStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(
                ErrorCode.GAME_MATCH_ILLEGAL_TRANSITION,
                UserMessages.GAME_MATCH_ILLEGAL_TRANSITION.formatted(status, target));
        }
        this.status = target;
    }

}
