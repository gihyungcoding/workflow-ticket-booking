ALTER TABLE performance ADD COLUMN poster_url VARCHAR(500);

CREATE TABLE performance_zone (
    id              BIGSERIAL PRIMARY KEY,
    performance_id  BIGINT NOT NULL REFERENCES performance(id) ON DELETE CASCADE,
    grade           VARCHAR(20) NOT NULL,
    area            VARCHAR(30) NOT NULL,
    area_note       VARCHAR(60),
    stage_order     INT NOT NULL CHECK (stage_order > 0),
    UNIQUE (performance_id, grade),
    UNIQUE (performance_id, stage_order)
);
