CREATE TYPE role_name AS ENUM (
    'ROLE_USER',
    'ROLE_ADMIN'
);

CREATE TYPE user_status AS ENUM (
    'ACTIVE',
    'LOCKED'
);

CREATE TYPE wallet_type AS ENUM (
    'CASH',
    'BANK',
    'E_WALLET',
    'CREDIT_CARD'
);

CREATE TYPE transaction_type AS ENUM (
    'INCOME',
    'EXPENSE'
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TABLE roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        role_name NOT NULL UNIQUE,
    description TEXT
);


CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id         BIGINT NOT NULL REFERENCES roles(id),
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) UNIQUE,
    phone_number    VARCHAR(20) UNIQUE,
    password        VARCHAR(255) NOT NULL,
    otp_code        VARCHAR(10),
    otp_expired_at  TIMESTAMP,
    status          user_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),

    -- Phải đăng ký bằng ít nhất email hoặc số điện thoại
    CONSTRAINT chk_users_identifier CHECK (email IS NOT NULL OR phone_number IS NOT NULL)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE refresh_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device      VARCHAR(255),
    token_hash  VARCHAR(255) NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT false,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE TRIGGER trg_refresh_tokens_updated_at
    BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE wallets (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    type        wallet_type NOT NULL,
    money       NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);

CREATE TRIGGER trg_wallets_updated_at
    BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    type        transaction_type NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_categories_user_name_type UNIQUE (user_id, name, type)
);

CREATE INDEX idx_categories_user_id ON categories(user_id);

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE transactions (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wallet_id         BIGINT NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    category_id       BIGINT NOT NULL REFERENCES categories(id),
    type              transaction_type NOT NULL,
    amount            NUMERIC(18,2) NOT NULL,
    transaction_date  DATE NOT NULL,
    note              VARCHAR(500),
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);

CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();