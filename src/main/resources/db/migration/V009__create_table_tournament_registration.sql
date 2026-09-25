-- A registration is CONFIRMED while the tournament has room and WAITLISTED once max_players is
-- reached. Withdrawing deletes the row, and the oldest waitlisted registration takes the freed
-- place (registered_at, then player_id to break ties). The waitlist is deleted when the tournament
-- leaves OPEN, so from then on every row is a confirmed player.
--
-- Capacity is not enforced here, since a CHECK cannot count rows: the service must apply it under
-- a lock on the tournament row, or two concurrent registrations can both take the last place.
CREATE TABLE league.tournament_registration
(
    tournament_id BIGINT      NOT NULL,
    player_id     BIGINT      NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- No default on purpose: CONFIRMED is the value that bypasses the capacity rule, so the
    -- service always chooses, and an insert that forgets to fails instead of overbooking.
    status        VARCHAR(20) NOT NULL,

    CONSTRAINT pk_tournament_registration PRIMARY KEY (tournament_id, player_id),

    CONSTRAINT fk_tournament_registration_tournament
        FOREIGN KEY (tournament_id) REFERENCES league.tournament (id)
            -- Prevent deleting a tournament that has registrations
            ON DELETE RESTRICT,

    CONSTRAINT fk_tournament_registration_player
        FOREIGN KEY (player_id) REFERENCES league.player (id)
            -- Prevent deleting a player who has registrations: they withdraw first, through the
            -- service, so their place goes to the waitlist. A cascade would free the place without
            -- promoting anyone.
            ON DELETE RESTRICT,

    CONSTRAINT ck_tournament_registration_status CHECK (status IN ('CONFIRMED', 'WAITLISTED'))
);

-- The primary key only serves lookups that start with tournament_id. This index serves a player's
-- registrations and the RESTRICT check when a player is deleted.
CREATE INDEX idx_tournament_registration_player ON league.tournament_registration (player_id);
