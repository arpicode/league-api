package io.arpicode.leagueapi.boardgame;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

@Entity
@Getter
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_game", schema = "league")
public class BoardGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NonNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_normalized", insertable = false, updatable = false)
    private String nameNormalized;

    @Setter
    @NonNull
    @Column(name = "min_players", nullable = false)
    private Short minPlayers;

    @Setter
    @NonNull
    @Column(name = "max_players", nullable = false)
    private Short maxPlayers;

    @Setter
    @Column(name = "avg_duration_min")
    private Short avgDurationMin;

    @Column(name = "created_at", nullable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private OffsetDateTime updatedAt;

}
