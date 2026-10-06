CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_user_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(1000) NOT NULL
);
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    price DECIMAL(15,0) NOT NULL,
    stock INT NOT NULL,
    sales_unit VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT ck_price CHECK (price > 0),
    CONSTRAINT ck_stock CHECK (stock >= 0)
);
CREATE INDEX idx_product_category ON products(category_id);
CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uq_cart_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_cart_quantity CHECK (quantity BETWEEN 1 AND 99)
);
CREATE TABLE customer_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    checkout_token VARCHAR(36) NOT NULL UNIQUE,
    recipient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(500) NOT NULL,
    note VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total DECIMAL(18,0) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT ck_order_total CHECK (total >= 0),
    CONSTRAINT ck_order_status CHECK (status IN ('PENDING','CONFIRMED','SHIPPING','COMPLETED','CANCELLED'))
);
CREATE INDEX idx_order_user ON customer_orders(user_id);
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(160) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    unit_price DECIMAL(15,0) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES customer_orders(id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_item_price CHECK (unit_price > 0)
);
