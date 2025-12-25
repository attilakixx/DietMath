CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE foods (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    calories_per_100g INTEGER NOT NULL CHECK (calories_per_100g >= 0),
    source VARCHAR(16) NOT NULL CHECK (source IN ('BUILTIN', 'USER')),
    external_id VARCHAR(64),
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE foods ADD CONSTRAINT foods_source_owner_check
    CHECK (
        (source = 'USER' AND user_id IS NOT NULL AND external_id IS NULL)
        OR (source = 'BUILTIN' AND user_id IS NULL AND external_id IS NOT NULL)
    );

CREATE UNIQUE INDEX foods_builtin_external_id_uk
    ON foods (external_id)
    WHERE source = 'BUILTIN';

CREATE INDEX foods_name_idx ON foods (name);

CREATE TABLE diary_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_id BIGINT NOT NULL REFERENCES foods(id) ON DELETE RESTRICT,
    entry_date DATE NOT NULL,
    grams NUMERIC(10, 2) NOT NULL CHECK (grams > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX diary_entries_user_date_idx
    ON diary_entries (user_id, entry_date);
