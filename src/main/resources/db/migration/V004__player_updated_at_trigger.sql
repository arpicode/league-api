CREATE TRIGGER trg_player_updated_at
    BEFORE UPDATE
    ON league.player
    FOR EACH ROW
EXECUTE FUNCTION shared.touch_updated_at();
