CREATE TABLE consumed_calories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_id BIGINT NOT NULL REFERENCES foods(id) ON DELETE RESTRICT,
    grams NUMERIC(10, 2) NOT NULL CHECK (grams > 0),
    calories NUMERIC(10, 2) NOT NULL CHECK (calories >= 0),
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX consumed_calories_user_time_idx
    ON consumed_calories (user_id, consumed_at);
