package vn.sunicoffee.service;

import java.time.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.domain.*;

@Service
public class OrderHistoryService {
    private final JdbcTemplate jdbc;
    public OrderHistoryService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Event(OrderStatus status,String actor,String note,LocalDateTime createdAt) {}
    @Transactional public void record(CustomerOrder order,String actor,String note) {
        jdbc.update("INSERT INTO order_events(order_id,status,actor,note,created_at) VALUES (?,?,?,?,?)",
            order.getId(),order.getStatus().name(),actor==null ? "Hệ thống" : actor,note,LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
    }
    @Transactional(readOnly=true) public List<Event> events(Long id) {
        return jdbc.query("SELECT status,actor,note,created_at FROM order_events WHERE order_id=? ORDER BY id",
            (rs,n) -> new Event(OrderStatus.valueOf(rs.getString(1)),rs.getString(2),rs.getString(3),rs.getTimestamp(4).toLocalDateTime()),id);
    }
}
