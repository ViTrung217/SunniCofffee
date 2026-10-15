CREATE TABLE google_accounts (
    subject VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_google_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);
