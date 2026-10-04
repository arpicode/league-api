package io.arpicode.leagueapi.gamematch;

import io.arpicode.leagueapi.player.Player;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "match_participant", schema = "league")
public class MatchParticipant {

    @EmbeddedId
    private MatchParticipantId id;

    // Derived identity, as in TournamentRegistration. The match's id is only known once its insert
    // runs, so it is left null here and copied in when the participant is persisted with the match.
    @MapsId("gameMatchId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_match_id", nullable = false)
    private GameMatch gameMatch;

    @MapsId("playerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    // A plain column rather than an association: both of V013's composite foreign keys run through
    // it, and it is always the match's own tournament, which is what lets V013 check that the player
    // is registered there.
    @Column(name = "tournament_id", nullable = false, updatable = false)
    private Long tournamentId;

    @Column(name = "seat_number", nullable = false, updatable = false)
    private Short seatNumber;

    MatchParticipant(@NonNull GameMatch gameMatch, @NonNull Player player, short seatNumber) {
        this.id = new MatchParticipantId(null, player.getId());
        this.gameMatch = gameMatch;
        this.player = player;
        this.tournamentId = gameMatch.getTournament().getId();
        this.seatNumber = seatNumber;
    }

}
