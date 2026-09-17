CREATE TABLE league.board_game
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name             VARCHAR(120) NOT NULL,
    name_normalized  VARCHAR(120) GENERATED ALWAYS AS (lower(name)) STORED,
    min_players      SMALLINT     NOT NULL,
    max_players      SMALLINT     NOT NULL,
    avg_duration_min SMALLINT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_board_game_name_normalized UNIQUE (name_normalized),
    CONSTRAINT ck_board_game_players CHECK (min_players >= 1 AND max_players >= min_players)
);
