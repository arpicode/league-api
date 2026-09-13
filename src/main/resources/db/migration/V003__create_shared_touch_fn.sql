CREATE SCHEMA IF NOT EXISTS shared;

CREATE OR REPLACE FUNCTION shared.touch_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = shared
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;
