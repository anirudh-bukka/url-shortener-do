-- Flyway baseline migration. Runs automatically on application startup and is
-- versioned/checksummed so schema changes are reproducible across environments.

-- Sequence used to mint globally-unique numeric ids. Because the database hands
-- out each value exactly once, Base62-encoding it yields collision-free aliases
-- even when many creation requests run concurrently.
CREATE SEQUENCE IF NOT EXISTS url_mapping_seq
    START WITH 1000000        -- start high so the first aliases are >= 4 chars
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 50;                 -- pre-allocate ids per connection for throughput

CREATE TABLE IF NOT EXISTS url_mapping (
    id               BIGINT        PRIMARY KEY,
    short_code       VARCHAR(16)   NOT NULL,
    long_url         VARCHAR(2048) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    hit_count        BIGINT        NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMPTZ
);

-- The UNIQUE constraint is the safety net for custom aliases: a duplicate insert
-- fails at the database, which the service translates into a 409 Conflict.
CREATE UNIQUE INDEX IF NOT EXISTS ux_url_mapping_short_code ON url_mapping (short_code);

-- Speeds up the optional "return existing alias for this long URL" dedup lookup.
CREATE INDEX IF NOT EXISTS ix_url_mapping_long_url ON url_mapping (long_url);
