CREATE TRIGGER trg_board_game_updated_at
    BEFORE UPDATE
    ON league.board_game
    FOR EACH ROW
EXECUTE FUNCTION shared.touch_updated_at();
