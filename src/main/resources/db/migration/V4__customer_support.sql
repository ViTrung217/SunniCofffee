CREATE TABLE support_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    customer_read_id BIGINT NOT NULL DEFAULT 0,
    admin_read_id BIGINT NOT NULL DEFAULT 0,
    last_message_id BIGINT NOT NULL DEFAULT 0,
    preview VARCHAR(200) NOT NULL DEFAULT '',
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_support_customer FOREIGN KEY (customer_id) REFERENCES app_users(id)
);
CREATE INDEX idx_support_updated ON support_conversations(updated_at, id);
CREATE TABLE support_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    from_admin BOOLEAN NOT NULL,
    content VARCHAR(1500) NOT NULL,
    request_token VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_support_thread FOREIGN KEY (conversation_id) REFERENCES support_conversations(id),
    CONSTRAINT fk_support_sender FOREIGN KEY (sender_id) REFERENCES app_users(id),
    CONSTRAINT uq_support_request UNIQUE(sender_id, request_token)
);
CREATE INDEX idx_support_messages ON support_messages(conversation_id, id);
CREATE INDEX idx_support_sender_time ON support_messages(sender_id, created_at);
