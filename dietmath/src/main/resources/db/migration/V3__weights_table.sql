ALTER TABLE users
    DROP COLUMN IF EXISTS weight,
    DROP COLUMN IF EXISTS goal_weight;

CREATE TABLE user_weights (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    weight NUMERIC(6, 2) NOT NULL CHECK (weight > 0),
    goal_weight NUMERIC(6, 2) CHECK (goal_weight > 0),
    goal_date DATE,
    calorie_strategy VARCHAR(16) NOT NULL CHECK (calorie_strategy IN ('DYNAMIC', 'FIXED')),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX user_weights_user_recorded_at_idx
    ON user_weights (user_id, recorded_at);
