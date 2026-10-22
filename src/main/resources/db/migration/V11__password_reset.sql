CREATE TABLE password_reset_tokens (
    user_id BIGINT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);
