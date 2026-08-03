-- Multi-session refresh tokens replace the old single-column-on-users approach.
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    token_hash  VARCHAR(255)  NOT NULL,
    expires_at  TIMESTAMP     NOT NULL,
    revoked     BOOLEAN       NOT NULL DEFAULT FALSE,
    device      VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_tokens (user_id);

ALTER TABLE users DROP COLUMN refresh_token_hash;
ALTER TABLE users DROP COLUMN refresh_token_expiry;

ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP;
