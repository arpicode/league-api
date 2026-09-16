CREATE TABLE league.player
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Named explicitly rather than left to Postgres: GlobalExceptionHandler maps these
    -- names to USERNAME_ALREADY_EXISTS / EMAIL_ALREADY_EXISTS, and auto-generated names
    -- change silently when the table is renamed or the constraint is re-created.
    CONSTRAINT uq_player_username UNIQUE (username),
    CONSTRAINT uq_player_email UNIQUE (email)
);
