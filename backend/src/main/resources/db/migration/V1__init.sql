-- Initial schema for SpendWise.

CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(120)  NOT NULL,
    email                 VARCHAR(190)  NOT NULL,
    password              VARCHAR(255)  NOT NULL,
    role                  VARCHAR(20)   NOT NULL DEFAULT 'USER',
    enabled               BOOLEAN       NOT NULL DEFAULT TRUE,
    refresh_token_hash    VARCHAR(255),
    refresh_token_expiry  TIMESTAMP,
    created_at            TIMESTAMP     NOT NULL,
    updated_at            TIMESTAMP     NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    color       VARCHAR(20),
    icon        VARCHAR(40),
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id     BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_category_user_name UNIQUE (user_id, name)
);

CREATE TABLE expenses (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(160)   NOT NULL,
    amount        NUMERIC(14,2)  NOT NULL,
    currency      VARCHAR(3)     NOT NULL DEFAULT 'INR',
    kind          VARCHAR(10)    NOT NULL DEFAULT 'EXPENSE',
    notes         VARCHAR(500),
    expense_date  DATE           NOT NULL,
    category_id   BIGINT         NOT NULL,
    user_id       BIGINT         NOT NULL,
    created_at    TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP      NOT NULL,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_expense_user_date ON expenses (user_id, expense_date);
CREATE INDEX idx_expense_user_category ON expenses (user_id, category_id);

CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    token_hash  VARCHAR(255)  NOT NULL,
    expires_at  TIMESTAMP     NOT NULL,
    used        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT fk_reset_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_reset_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_reset_token_user ON password_reset_tokens (user_id);
