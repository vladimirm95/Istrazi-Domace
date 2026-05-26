CREATE TABLE reviews (
                         id             UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
                         reviewer_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         target_user_id UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         score          SMALLINT  NOT NULL CHECK (score BETWEEN 1 AND 5),
                         comment        TEXT,
                         created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                         CONSTRAINT unique_review UNIQUE (reviewer_id, target_user_id)
);

CREATE INDEX idx_reviews_target ON reviews(target_user_id);

CREATE TABLE friendships (
                             id           UUID              PRIMARY KEY DEFAULT uuid_generate_v4(),
                             requester_id UUID              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             receiver_id  UUID              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             status       friendship_status NOT NULL DEFAULT 'PENDING',
                             created_at   TIMESTAMP         NOT NULL DEFAULT NOW(),
                             CONSTRAINT no_self_friendship  CHECK (requester_id != receiver_id),
    CONSTRAINT unique_friendship   UNIQUE (requester_id, receiver_id)
);

CREATE INDEX idx_friendships_receiver  ON friendships(receiver_id);
CREATE INDEX idx_friendships_requester ON friendships(requester_id);

CREATE TABLE messages (
                          id          UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
                          sender_id   UUID      REFERENCES users(id) ON DELETE SET NULL,
                          receiver_id UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          content     TEXT      NOT NULL,
                          is_read     BOOLEAN   NOT NULL DEFAULT false,
                          sent_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                          created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation ON messages(sender_id, receiver_id);
CREATE INDEX idx_messages_receiver     ON messages(receiver_id, is_read);

CREATE TYPE notification_type AS ENUM (
    'FRIEND_REQUEST',
    'FRIEND_ACCEPTED',
    'NEW_POST',
    'NEW_REVIEW'
);

CREATE TABLE notifications (
                               id         UUID              PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id    UUID              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type       notification_type NOT NULL,
                               payload    JSONB,
                               is_read    BOOLEAN           NOT NULL DEFAULT false,
                               created_at TIMESTAMP         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);