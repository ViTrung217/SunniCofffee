package vn.sunicoffee;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

class MigrationUpgradeTest {
    @Test void upgradePreservesExistingDataAndCreatesOpeningBalanceOnlyOnce() throws Exception {
        String url="jdbc:h2:mem:upgrade-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("1").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO app_users(full_name,email,password_hash,role,enabled) VALUES ('Existing','existing@example.test','unchanged','ADMIN',TRUE)");
            s.executeUpdate("INSERT INTO categories(code,name,description) VALUES ('OLD','Old','Existing')");
            s.executeUpdate("INSERT INTO products(sku,name,description,price,stock,sales_unit,active,category_id) VALUES ('OLD-1','Existing','Existing',123000,7,'unit',TRUE,1)");
            s.executeUpdate("INSERT INTO customer_orders(user_id,checkout_token,recipient_name,phone,address,note,status,total,created_at) VALUES (1,'old-token','Existing','0900000000','Existing address','','COMPLETED',123000,CURRENT_TIMESTAMP)");
        }
        var flyway=Flyway.configure().dataSource(url,"sa","").load();flyway.migrate();flyway.migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            try(var rs=s.executeQuery("SELECT stock,price FROM products")) {rs.next();assertThat(rs.getInt(1)).isEqualTo(7);assertThat(rs.getBigDecimal(2)).isEqualByComparingTo("123000");}
            try(var rs=s.executeQuery("SELECT COUNT(*),SUM(quantity_delta) FROM stock_movements")) {rs.next();assertThat(rs.getLong(1)).isEqualTo(1);assertThat(rs.getLong(2)).isEqualTo(7);}
            try(var rs=s.executeQuery("SELECT password_hash,auth_version FROM app_users")) {rs.next();assertThat(rs.getString(1)).isEqualTo("unchanged");assertThat(rs.getLong(2)).isZero();}
            try(var rs=s.executeQuery("SELECT completed_at,payment_method,payment_status,received_amount FROM customer_orders")) {rs.next();assertThat(rs.getTimestamp(1)).isNull();assertThat(rs.getString(2)).isEqualTo("COD");assertThat(rs.getString(3)).isEqualTo("UNPAID");assertThat(rs.getBigDecimal(4)).isEqualByComparingTo("0");}
        }
    }
    @Test void paymentMigrationPreservesSettledCodAndDoesNotInventOnlineReceipts() throws Exception {
        String url="jdbc:h2:mem:payment-upgrade-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("4").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO app_users(full_name,email,password_hash,role,enabled) VALUES ('Existing','cod@example.test','unchanged','CUSTOMER',TRUE)");
            s.executeUpdate("INSERT INTO customer_orders(user_id,checkout_token,recipient_name,phone,address,note,status,total,created_at,cod_settled_at) VALUES (1,'paid-cod','Existing','0900000000','Existing address','','COMPLETED',123000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        }
        Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT payment_method,payment_status,received_amount,paid_at,cod_settled_at FROM customer_orders")) {r.next();assertThat(r.getString(1)).isEqualTo("COD");assertThat(r.getString(2)).isEqualTo("PAID");assertThat(r.getBigDecimal(3)).isEqualByComparingTo("123000");assertThat(r.getTimestamp(4)).isEqualTo(r.getTimestamp(5));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM payment_receipts")) {r.next();assertThat(r.getInt(1)).isZero();}
        }
    }
}
