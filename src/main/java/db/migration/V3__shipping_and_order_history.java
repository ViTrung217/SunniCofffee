package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Preserve existing orders; never invent past shipment or settlement dates. */
public class V3__shipping_and_order_history extends BaseJavaMigration {
    @Override public void migrate(Context context) throws Exception {
        var connection=context.getConnection();
        boolean mysql=connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL");
        try(var s=connection.createStatement()) {
            s.execute("ALTER TABLE customer_orders "+(mysql ? "DROP CHECK " : "DROP CONSTRAINT ")+"ck_order_status");
            s.execute("ALTER TABLE customer_orders ADD CONSTRAINT ck_order_status CHECK (status IN ('PENDING','CONFIRMED','SHIPPING','COMPLETED','CANCELLED','DELIVERY_FAILED','RETURNING','RETURNED'))");
            s.execute("ALTER TABLE stock_movements "+(mysql ? "DROP CHECK " : "DROP CONSTRAINT ")+"ck_stock_type");
            s.execute("ALTER TABLE stock_movements ADD CONSTRAINT ck_stock_type CHECK (type IN ('OPENING','RECEIPT','ISSUE','SALE','CANCEL_RETURN','DELIVERY_RETURN'))");
            s.execute("ALTER TABLE customer_orders ADD COLUMN shipping_fee DECIMAL(15,0) NOT NULL DEFAULT 0");
            s.execute("ALTER TABLE customer_orders ADD COLUMN carrier VARCHAR(100) NOT NULL DEFAULT ''");
            s.execute("ALTER TABLE customer_orders ADD COLUMN tracking_code VARCHAR(100) NOT NULL DEFAULT ''");
            s.execute("ALTER TABLE customer_orders ADD COLUMN shipped_at TIMESTAMP NULL");
            s.execute("ALTER TABLE customer_orders ADD COLUMN returned_at TIMESTAMP NULL");
            s.execute("ALTER TABLE customer_orders ADD COLUMN cod_settled_at TIMESTAMP NULL");
            s.execute("ALTER TABLE customer_orders ADD CONSTRAINT ck_shipping_fee CHECK (shipping_fee >= 0)");
            s.execute("ALTER TABLE order_items ADD COLUMN restocked_quantity INT NULL");
            s.execute("ALTER TABLE order_items ADD CONSTRAINT ck_restocked CHECK (restocked_quantity IS NULL OR (restocked_quantity >= 0 AND restocked_quantity <= quantity))");
            s.execute("CREATE TABLE order_events (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL, actor VARCHAR(190) NOT NULL, note VARCHAR(1000) NOT NULL, created_at TIMESTAMP NOT NULL, CONSTRAINT fk_event_order FOREIGN KEY(order_id) REFERENCES customer_orders(id))");
            s.execute("CREATE INDEX idx_event_order ON order_events(order_id,id)");
            s.execute("CREATE INDEX idx_order_status_created ON customer_orders(status,created_at)");
        }
    }
}
