CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reset_token VARCHAR(255) UNIQUE,
    reset_token_expiry TIMESTAMP,
    verification_token VARCHAR(255),
    verification_token_expiry TIMESTAMP,
    enabled BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    lock_time TIMESTAMP,
    last_login_ip VARCHAR(45),
    last_login_time TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    suspension_end_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    used BOOLEAN DEFAULT FALSE,
    revoked BOOLEAN DEFAULT FALSE,
    expiry_date TIMESTAMP NOT NULL,
    user_id BIGINT REFERENCES users(id)
);