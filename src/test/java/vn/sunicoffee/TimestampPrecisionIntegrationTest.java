package vn.sunicoffee;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TimestampPrecisionIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Test void preservesMicrosecondsForImportedBusinessTimestamps() {
        var expected=LocalDateTime.of(2026,10,28,12,34,56,123456000);
        jdbc.update("INSERT INTO activity_seed_runs(seed_key,completed_at) VALUES (?,?)","precision-regression",expected);
        var actual=jdbc.queryForObject("SELECT completed_at FROM activity_seed_runs WHERE seed_key=?",LocalDateTime.class,"precision-regression");
        assertThat(actual).isEqualTo(expected);
    }
    @Test void allBusinessTimestampColumnsSupportAtLeastMicrosecondPrecision() {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            boolean mysql=connection.getMetaData().getDatabaseProductName().equals("MySQL");
            // MySQL Connector/J may report DECIMAL_DIGITS=0 for temporal columns;
            // query the server's actual schema precision instead.
            String schema=mysql?"DATABASE()":"'public'";
            int checked=0;
            try(var statement=connection.createStatement();var columns=statement.executeQuery(
                    "SELECT table_name,column_name,datetime_precision FROM information_schema.columns WHERE table_schema="+schema
                    +" AND LOWER(data_type)='timestamp' AND table_name<>'flyway_schema_history'")) {
                while(columns.next()) {
                    assertThat(columns.getInt(3)).as(columns.getString(1)+"."+columns.getString(2)).isGreaterThanOrEqualTo(6);
                    checked++;
                }
            }
            assertThat(checked).isGreaterThan(0);
            return null;
        });
    }
}
