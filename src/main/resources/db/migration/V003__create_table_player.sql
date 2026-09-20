CREATE TABLE league.player
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username            VARCHAR(50)  NOT NULL,
    -- Mirrors board_game.name_normalized: the stored username keeps the casing the player
    -- chose, while uniqueness is enforced on the lower-cased copy the database derives.
    username_normalized TEXT GENERATED ALWAYS AS (lower(username)) STORED,
    email               VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Named explicitly rather than left to Postgres: GlobalExceptionHandler maps these
    -- names to USERNAME_ALREADY_EXISTS / EMAIL_ALREADY_EXISTS, and auto-generated names
    -- change silently when the table is renamed or the constraint is re-created.
    CONSTRAINT uq_player_username_normalized UNIQUE (username_normalized),
    -- Email stays normalized in the application (PlayerRequest lower-cases it), so the
    -- column itself is already canonical and needs no generated counterpart.
    CONSTRAINT uq_player_email UNIQUE (email)
);
