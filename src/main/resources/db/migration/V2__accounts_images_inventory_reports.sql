ALTER TABLE app_users ADD COLUMN phone VARCHAR(20) NOT NULL DEFAULT '';
ALTER TABLE app_users ADD COLUMN address VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE app_users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE account_admin_guard (id INT PRIMARY KEY);
INSERT INTO account_admin_guard(id) VALUES (1);

ALTER TABLE products ADD COLUMN has_image BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE product_images (
    product_id BIGINT PRIMARY KEY,
    media_type VARCHAR(30) NOT NULL,
    content LONGBLOB NOT NULL,
    CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES products(id)
);

ALTER TABLE customer_orders ADD COLUMN completed_at TIMESTAMP NULL;
CREATE INDEX idx_order_completed ON customer_orders(status, completed_at);

CREATE TABLE stock_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    order_id BIGINT NULL,
    type VARCHAR(20) NOT NULL,
    quantity_delta INT NOT NULL,
    balance_after INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    request_token VARCHAR(36) NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_stock_actor FOREIGN KEY (actor_id) REFERENCES app_users(id),
    CONSTRAINT fk_stock_order FOREIGN KEY (order_id) REFERENCES customer_orders(id),
    CONSTRAINT uq_stock_order UNIQUE (order_id, product_id, type),
    CONSTRAINT ck_stock_balance CHECK (balance_after >= 0),
    CONSTRAINT ck_stock_delta CHECK (quantity_delta <> 0),
    CONSTRAINT ck_stock_type CHECK (type IN ('OPENING','RECEIPT','ISSUE','SALE','CANCEL_RETURN'))
);
CREATE INDEX idx_stock_created ON stock_movements(created_at);
INSERT INTO stock_movements(product_id,type,quantity_delta,balance_after,reason,created_at)
SELECT id,'OPENING',stock,stock,'Số dư khi nâng cấp phiên bản 0.2',CURRENT_TIMESTAMP FROM products WHERE stock > 0;
