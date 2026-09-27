package io.arpicode.leagueapi.tournament;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class TournamentPlayerId implements Serializable {
    private Long tournamentId;
    private Long playerId;
}