CREATE TABLE performance (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    venue            VARCHAR(200) NOT NULL,
    start_at         TIMESTAMPTZ NOT NULL,
    open_at          TIMESTAMPTZ NOT NULL,
    close_at         TIMESTAMPTZ NOT NULL,
    total_seats      INT NOT NULL CHECK (total_seats > 0),
    available_seats  INT NOT NULL CHECK (available_seats >= 0 AND available_seats <= total_seats),
    cancelled        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_performance_start_at ON performance (start_at);
