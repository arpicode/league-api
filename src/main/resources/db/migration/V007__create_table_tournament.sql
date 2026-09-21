CREATE TABLE league.tournament
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    name_normalized TEXT GENERATED ALWAYS AS (lower(name)) STORED,
    board_game_id   BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    max_players     SMALLINT,
    starts_on       DATE,
    ends_on         DATE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Optimistic lock (@Version): guards concurrent updates to this row.
    -- It is here for the status transitions, so two organizers cannot both win.
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_tournament_name_normalized UNIQUE (name_normalized),

    CONSTRAINT fk_tournament_board_game
        FOREIGN KEY (board_game_id) REFERENCES league.board_game (id)
            -- Prevent deleting a game that is/was used in a tournament
            ON DELETE RESTRICT,

    CONSTRAINT ck_tournament_status CHECK (
        status IN ('DRAFT', 'OPEN', 'IN_PROGRESS', 'CLOSED', 'CANCELLED')
        ),

    CONSTRAINT ck_tournament_max_players CHECK (max_players IS NULL OR max_players >= 2),

    -- An end date without a start date makes no sense.
    --
    -- Note: 'ends_on >= starts_on' evaluates to NULL if starts_on is NULL,
    -- and a CHECK only rejects FALSE (needs explicit clause).
    CONSTRAINT ck_tournament_dates CHECK (
        (ends_on IS NULL OR starts_on IS NOT NULL)
            AND (ends_on IS NULL OR ends_on >= starts_on)
        ),

    -- Keystone constraint: allows to reference the PAIR (tournament, game),
    -- and therefore prevents a Wingspan match inside a Catan tournament.
    --
    -- PostgreSQL requires this unique constraint even though id is already the primary key.
    CONSTRAINT uq_tournament_id_board_game_id UNIQUE (id, board_game_id)
);

-- Postgres does not automatically index foreign key columns.
CREATE INDEX idx_tournament_board_game ON league.tournament (board_game_id);
