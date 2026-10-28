package db.migration;

import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** MySQL TIMESTAMP defaults to zero fractional digits; H2 defaults to six. */
public class V12__preserve_timestamp_precision extends BaseJavaMigration {
    @Override public void migrate(Context context) throws Exception {
        var connection=context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL")) return;
        record Column(String table,String name,boolean nullable) {}
        var columns=List.of(
            new Column("customer_orders","created_at",false),
            new Column("customer_orders","completed_at",true),
            new Column("customer_orders","shipped_at",true),
            new Column("customer_orders","returned_at",true),
            new Column("customer_orders","cod_settled_at",true),
            new Column("customer_orders","paid_at",true),
            new Column("customer_orders","last_payment_check",true),
            new Column("stock_movements","created_at",false),
            new Column("order_events","created_at",false),
            new Column("support_conversations","updated_at",false),
            new Column("support_messages","created_at",false),
            new Column("payment_receipts","transaction_at",false),
            new Column("payment_receipts","recorded_at",false),
            new Column("product_favorites","created_at",false),
            new Column("product_reviews","updated_at",false),
            new Column("activity_seed_runs","completed_at",true));
        try(var statement=connection.createStatement()) {
            for(var column:columns) statement.execute("ALTER TABLE `"+column.table()+"` MODIFY COLUMN `"+column.name()+"` TIMESTAMP(6) "+(column.nullable()?"NULL":"NOT NULL"));
        }
    }
}
