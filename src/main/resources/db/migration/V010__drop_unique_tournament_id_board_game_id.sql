-- V007 added this constraint so a match could reference the (tournament, board game) pair and never
-- disagree with its tournament's game. game_match has no board_game_id: a match gets its game
-- through its tournament, so nothing references the pair and the constraint only costs an index.
ALTER TABLE league.tournament
    DROP CONSTRAINT uq_tournament_id_board_game_id;
