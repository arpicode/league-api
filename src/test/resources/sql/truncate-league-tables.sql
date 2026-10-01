-- Run after every @CommitsData test. Naming every table in one statement lets Postgres truncate
-- across their foreign keys without CASCADE, so a new table that references one of these fails
-- this statement until it is added here, rather than being emptied silently or left behind.
TRUNCATE league.game_match, league.tournament_registration, league.tournament, league.board_game, league.player;
