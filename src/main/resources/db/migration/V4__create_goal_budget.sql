
CREATE TABLE budgets (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id     BIGINT NOT NULL REFERENCES categories(id),
    amount_limit    NUMERIC(18,2) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT chk_budget_amount_positive CHECK (amount_limit > 0),
    CONSTRAINT chk_budget_date_range CHECK (end_date >= start_date)
);

CREATE INDEX idx_budgets_user_id ON budgets(user_id);
CREATE INDEX idx_budgets_category_id ON budgets(category_id);
CREATE INDEX idx_budgets_date_range ON budgets(start_date, end_date);

CREATE TRIGGER trg_budgets_updated_at
    BEFORE UPDATE ON budgets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE goals (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    target_amount   NUMERIC(18,2) NOT NULL,
    current_amount  NUMERIC(18,2) NOT NULL DEFAULT 0,
    deadline        DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT chk_goal_target_positive CHECK (target_amount > 0),
    CONSTRAINT chk_goal_current_non_negative CHECK (current_amount >= 0)
);

CREATE INDEX idx_goals_user_id ON goals(user_id);

CREATE TRIGGER trg_goals_updated_at
    BEFORE UPDATE ON goals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();