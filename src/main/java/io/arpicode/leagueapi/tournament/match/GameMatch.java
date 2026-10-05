package io.arpicode.leagueapi.tournament.match;

import io.arpicode.leagueapi.boardgame.BoardGame;
import io.arpicode.leagueapi.player.Player;
import io.arpicode.leagueapi.shared.error.BusinessException;
import io.arpicode.leagueapi.shared.error.ErrorCode;
import io.arpicode.leagueapi.shared.error.UserMessages;
import io.arpicode.leagueapi.tournament.Tournament;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // Part of their match, as V013's cascade says: created with it, and never without it.
    @OneToMany(mappedBy = "gameMatch", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seatNumber")
    private List<MatchParticipant> participants = new ArrayList<>();

    // The caller checks the tournament first, under its lock: that it is IN_PROGRESS, that the match
    // day falls within its dates, and that every player is registered in it. The players are seated
    // in the order given, the first one taking seat 1.
    public GameMatch(@NonNull Tournament tournament, Short roundNumber, OffsetDateTime scheduledAt,
                     @NonNull List<Player> players) {
        assertPlayerCount(tournament.getBoardGame(), players.size());
        this.tournament = tournament;
        this.roundNumber = roundNumber;
        this.scheduledAt = scheduledAt;
        for (int seat = 1; seat <= players.size(); seat++) {
            participants.add(new MatchParticipant(this, players.get(seat - 1), (short) seat));
        }
    }

    public List<MatchParticipant> getParticipants() {
        return Collections.unmodifiableList(participants);
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

    // V013 cannot count rows, so this is the only place the game's player range is enforced. The game
    // comes through the tournament, which can no longer change it once matches exist.
    private static void assertPlayerCount(BoardGame boardGame, int playerCount) {
        if (playerCount < boardGame.getMinPlayers() || playerCount > boardGame.getMaxPlayers()) {
            throw new BusinessException(
                ErrorCode.GAME_MATCH_PLAYER_COUNT_OUT_OF_RANGE,
                UserMessages.GAME_MATCH_PLAYER_COUNT_OUT_OF_RANGE.formatted(
                    boardGame.getName(), boardGame.getMinPlayers(), boardGame.getMaxPlayers(), playerCount));
        }
    }

}
