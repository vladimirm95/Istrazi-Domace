CREATE TABLE places (
                        id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        author_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        title       VARCHAR(100) NOT NULL,
                        description TEXT,
                        location    VARCHAR(200),
                        avg_rating  NUMERIC(3,2) NOT NULL DEFAULT 0.00,
                        created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_places_author ON places(author_id);

CREATE TABLE products (
                          id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          author_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          title       VARCHAR(100) NOT NULL,
                          description TEXT,
                          category    VARCHAR(50),
                          avg_rating  NUMERIC(3,2) NOT NULL DEFAULT 0.00,
                          created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_author   ON products(author_id);
CREATE INDEX idx_products_category ON products(category);