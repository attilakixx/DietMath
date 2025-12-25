ALTER TABLE users
    ADD COLUMN weight NUMERIC(6, 2) CHECK (weight > 0),
    ADD COLUMN height INTEGER CHECK (height > 0),
    ADD COLUMN birth_date DATE,
    ADD COLUMN goal_weight NUMERIC(6, 2) CHECK (goal_weight > 0);

CREATE OR REPLACE FUNCTION prevent_birth_date_update()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.birth_date IS DISTINCT FROM NEW.birth_date THEN
        RAISE EXCEPTION 'birth_date is immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_birth_date_immutable
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION prevent_birth_date_update();
