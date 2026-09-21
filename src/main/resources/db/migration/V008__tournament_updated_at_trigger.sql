CREATE TRIGGER trg_tournament_updated_at
    BEFORE UPDATE
    ON league.tournament
    FOR EACH ROW
EXECUTE FUNCTION shared.touch_updated_at();