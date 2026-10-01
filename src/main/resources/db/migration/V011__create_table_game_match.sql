CREATE TABLE league.game_match
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tournament_id BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    round_number  SMALLINT,
    scheduled_at  TIMESTAMPTZ,
    started_at    TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Optimistic lock (@Version): guards two organizers from entering the same result at the same time
    version       BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT fk_game_match_tournament
        FOREIGN KEY (tournament_id) REFERENCES league.tournament (id)
            -- Prevent deleting a tournament while matches reference it
            ON DELETE RESTRICT,

    CONSTRAINT ck_game_match_status CHECK (
        status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
        ),

    CONSTRAINT ck_game_match_round_number CHECK (round_number IS NULL OR round_number > 0),

    CONSTRAINT ck_game_match_completed_at_status CHECK (
        (completed_at IS NULL AND status != 'COMPLETED')
            OR (status = 'COMPLETED' AND completed_at IS NOT NULL)
        ),

    -- started_at is set when the match starts and stays set once it completes. A match can be
    -- cancelled before or after it starts, so CANCELLED accepts both.
    CONSTRAINT ck_game_match_started_at_status CHECK (
        (status = 'SCHEDULED' AND started_at IS NULL)
            OR (status IN ('IN_PROGRESS', 'COMPLETED') AND started_at IS NOT NULL)
            OR status = 'CANCELLED'
        ),

    -- A completed date without a started date makes no sense.
    --
    -- Note: 'completed_at >= started_at' evaluates to NULL if started_at is NULL,
    -- and a CHECK only rejects FALSE (needs explicit clause).
    CONSTRAINT ck_game_match_start_before_end CHECK (
        (completed_at IS NULL OR started_at IS NOT NULL)
            AND (completed_at IS NULL OR completed_at >= started_at)
        ),

    -- Lets match_participant reference the PAIR (match, tournament), so its tournament_id is the
    -- match's own and can also point at a registration in that tournament.
    --
    -- PostgreSQL requires this unique constraint even though id is already the primary key.
    CONSTRAINT uq_game_match_id_tournament_id UNIQUE (id, tournament_id)
);

-- Serves a tournament's matches, filtered by status or not, and the RESTRICT check when a
-- tournament is deleted.
CREATE INDEX idx_game_match_tournament_status ON league.game_match (tournament_id, status);
