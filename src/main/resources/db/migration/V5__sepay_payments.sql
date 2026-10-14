ALTER TABLE customer_orders ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'COD';
ALTER TABLE customer_orders ADD COLUMN payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE customer_orders ADD COLUMN payment_code VARCHAR(24);
ALTER TABLE customer_orders ADD COLUMN received_amount DECIMAL(18,0) NOT NULL DEFAULT 0;
ALTER TABLE customer_orders ADD COLUMN paid_at TIMESTAMP NULL;
ALTER TABLE customer_orders ADD COLUMN last_payment_check TIMESTAMP NULL;
ALTER TABLE customer_orders ADD COLUMN payment_sandbox BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE customer_orders ADD COLUMN sepay_bank_account_id VARCHAR(36);
ALTER TABLE customer_orders ADD COLUMN sepay_account_number VARCHAR(64);
ALTER TABLE customer_orders ADD COLUMN sepay_qr_account VARCHAR(64);
ALTER TABLE customer_orders ADD COLUMN sepay_bank VARCHAR(40);
ALTER TABLE customer_orders ADD COLUMN sepay_holder VARCHAR(100);
ALTER TABLE customer_orders ADD COLUMN sepay_prefix VARCHAR(40);
ALTER TABLE customer_orders ADD COLUMN sepay_va VARCHAR(64);
CREATE UNIQUE INDEX uq_order_payment_code ON customer_orders(payment_code);
UPDATE customer_orders SET payment_status='PAID', received_amount=total, paid_at=cod_settled_at WHERE cod_settled_at IS NOT NULL;
CREATE TABLE payment_receipts (
    provider_transaction_id VARCHAR(36) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(18,0) NOT NULL,
    reference_number VARCHAR(160) NOT NULL,
    transaction_at TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_receipt_order FOREIGN KEY(order_id) REFERENCES customer_orders(id)
);
CREATE INDEX idx_payment_receipt_order ON payment_receipts(order_id);
