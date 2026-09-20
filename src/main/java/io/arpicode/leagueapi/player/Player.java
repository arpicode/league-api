package io.arpicode.leagueapi.player;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

@Entity
@Getter
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "player", schema = "league")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NonNull
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "username_normalized", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private String usernameNormalized;

    @Setter
    @NonNull
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "created_at", nullable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime createdAt;

    // Maintained entirely by the database: the trg_player_updated_at trigger (V004) sets
    // the column, @Generated tells Hibernate to read the new value back, and that read only
    // happens because update() calls saveAndFlush. Break any link in that chain and this
    // field silently stops advancing.
    @Column(name = "updated_at", nullable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private OffsetDateTime updatedAt;

}
