-- A participant is a registration of the match's tournament. Two composite foreign keys enforce it:
-- (game_match_id, tournament_id) makes tournament_id the match's own, and (tournament_id, player_id)
-- points at a registration in that tournament. Matches only exist from IN_PROGRESS, once the
-- waitlist is deleted and withdrawals are closed, so every registration referenced here is
-- confirmed and stays.
--
-- Not enforced here, since a CHECK cannot count rows or read another table:
--   - the number of participants within the board game's min/max players: the service applies it
--     when it creates the match with its participants;
--   - score and outcome set for every participant once the match is COMPLETED: complete() takes
--     the results.
CREATE TABLE league.match_participant
(
    game_match_id BIGINT   NOT NULL,
    tournament_id BIGINT   NOT NULL,
    player_id     BIGINT   NOT NULL,
    seat_number   SMALLINT NOT NULL,
    score         INTEGER,
    outcome       VARCHAR(10),

    CONSTRAINT pk_match_participant PRIMARY KEY (game_match_id, player_id),

    CONSTRAINT fk_match_participant_game_match
        FOREIGN KEY (game_match_id, tournament_id) REFERENCES league.game_match (id, tournament_id)
            -- Participants are part of their match and mean nothing without it
            ON DELETE CASCADE,

    CONSTRAINT fk_match_participant_registration
        FOREIGN KEY (tournament_id, player_id)
            REFERENCES league.tournament_registration (tournament_id, player_id)
            -- A played match is a historical record: neither the registration nor, through V009's
            -- RESTRICT, the player can be deleted while a match still lists them
            ON DELETE RESTRICT,

    CONSTRAINT uq_match_participant_seat UNIQUE (game_match_id, seat_number),

    CONSTRAINT ck_match_participant_seat_number CHECK (seat_number > 0),

    CONSTRAINT ck_match_participant_outcome CHECK (outcome IS NULL OR outcome IN ('WIN', 'LOSS', 'DRAW'))
);

-- The primary key only serves lookups that start with game_match_id. This index serves a player's
-- matches, across tournaments or within one, and the RESTRICT check when a registration is deleted.
CREATE INDEX idx_match_participant_player_tournament ON league.match_participant (player_id, tournament_id);
