-- A budget is a monthly spending cap per user per category. One row per user+category;
-- "monthly" is the only period supported for now (kept simple on purpose).
CREATE TABLE budgets (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    monthly_limit  NUMERIC(14,2)  NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT uk_budget_user_category UNIQUE (user_id, category_id)
);

CREATE INDEX idx_budget_user ON budgets (user_id);
