CREATE TABLE seat (
    id              BIGSERIAL PRIMARY KEY,
    performance_id  BIGINT NOT NULL REFERENCES performance(id),
    grade           VARCHAR(50) NOT NULL,
    seat_row        VARCHAR(10) NOT NULL,
    seat_number     INT NOT NULL CHECK (seat_number > 0),
    seat_label      VARCHAR(30) NOT NULL,
    price           INT NOT NULL CHECK (price >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (performance_id, seat_row, seat_number)
);

CREATE INDEX idx_seat_performance_id ON seat (performance_id);
