CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
                       username      VARCHAR(50)  NOT NULL UNIQUE,
                       email         VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       is_active     BOOLEAN      NOT NULL DEFAULT true,
                       deleted_at    TIMESTAMP,
                       created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);