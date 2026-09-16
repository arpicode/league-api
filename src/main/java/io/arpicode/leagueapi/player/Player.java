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
    Long id;

    @Setter
    @NonNull
    @Column(name = "username", nullable = false, unique = true, length = 50)
    String username;

    @Setter
    @NonNull
    @Column(name = "email", nullable = false, unique = true)
    String email;

    @Column(name = "created_at", nullable = false)
    @Generated(event = EventType.INSERT)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    OffsetDateTime updatedAt;

}
