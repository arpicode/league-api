-- V009's now() is the start of the transaction, not the moment of the insert. The waitlist follows
-- registered_at, but registrations take their turn on the tournament row's lock, so a transaction
-- that began first and got the lock second ranked ahead of the registration that came before it,
-- and registrations sharing a transaction tied, leaving the player id to rank them.
-- clock_timestamp() is read when the row is inserted, which is under that lock, so registered_at
-- follows the order the registrations were made in. Rows already stored keep their value.
ALTER TABLE league.tournament_registration
    ALTER COLUMN registered_at SET DEFAULT clock_timestamp();
