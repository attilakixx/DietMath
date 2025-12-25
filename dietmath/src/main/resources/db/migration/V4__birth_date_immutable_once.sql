DROP TRIGGER IF EXISTS users_birth_date_immutable ON users;
DROP FUNCTION IF EXISTS prevent_birth_date_update();

CREATE OR REPLACE FUNCTION prevent_birth_date_update()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.birth_date IS NOT NULL AND NEW.birth_date IS DISTINCT FROM OLD.birth_date THEN
        RAISE EXCEPTION 'birth_date is immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_birth_date_immutable
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION prevent_birth_date_update();
