CREATE TYPE entity_type AS ENUM ('PLACE', 'PRODUCT');

CREATE TABLE photos (
                        id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
                        entity_id   UUID         NOT NULL,
                        entity_type entity_type  NOT NULL,
                        url         VARCHAR(500) NOT NULL,
                        sort_order  SMALLINT     NOT NULL DEFAULT 0,
                        created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
                        CONSTRAINT max_sort_order CHECK (sort_order BETWEEN 0 AND 3)
);

CREATE INDEX idx_photos_entity ON photos(entity_id, entity_type);