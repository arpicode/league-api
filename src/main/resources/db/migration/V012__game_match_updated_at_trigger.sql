CREATE TRIGGER trg_game_match_updated_at
    BEFORE UPDATE
    ON league.game_match
    FOR EACH ROW
EXECUTE FUNCTION shared.touch_updated_at();