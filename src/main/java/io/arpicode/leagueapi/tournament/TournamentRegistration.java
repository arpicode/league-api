package io.arpicode.leagueapi.tournament;

import io.arpicode.leagueapi.player.Player;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tournament_registration", schema = "league")
public class TournamentRegistration implements Persistable<TournamentPlayerId> {

    @EmbeddedId
    private TournamentPlayerId id;

    @Transient
    private boolean isNew = true;

    // Derived identity: each association shares its column with the matching id field, so a
    // registration is built from the entities themselves and the two Long ids cannot be swapped.
    @MapsId("tournamentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @MapsId("playerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Generated(event = EventType.INSERT)
    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TournamentRegistrationStatus status;

    public TournamentRegistration(@NonNull Tournament tournament, @NonNull Player player,
                                  @NonNull TournamentRegistrationStatus status) {
        this.id = new TournamentPlayerId(tournament.getId(), player.getId());
        this.tournament = tournament;
        this.player = player;
        this.status = status;
    }

    // Only the service chooses which row to promote, and only among waitlisted rows, so failing here
    // is a sign of a bug in the service logic, not a client error: hence no BusinessException.
    public void promote() {
        if (status != TournamentRegistrationStatus.WAITLISTED) {
            throw new IllegalStateException(
                "Cannot promote registration (tournament %d, player %d): status is %s, expected WAITLISTED"
                    .formatted(id.getTournamentId(), id.getPlayerId(), status));
        }
        status = TournamentRegistrationStatus.CONFIRMED;
    }

    public boolean isConfirmed() {
        return status == TournamentRegistrationStatus.CONFIRMED;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

}
