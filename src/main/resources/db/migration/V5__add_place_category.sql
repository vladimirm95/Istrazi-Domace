ALTER TABLE places
    ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'OSTALO';

CREATE INDEX idx_places_category ON places(category);
-- 8 foto?
ALTER TABLE photos
DROP CONSTRAINT IF EXISTS max_sort_order;

ALTER TABLE photos
    ADD CONSTRAINT max_sort_order CHECK (sort_order BETWEEN 0 AND 7);