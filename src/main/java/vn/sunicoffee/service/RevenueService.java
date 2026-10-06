package vn.sunicoffee.service;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class RevenueService {
    private final JdbcTemplate jdbc;
    public RevenueService(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public record Daily(LocalDate day,long orderCount,BigDecimal revenue) {
        public int percent(BigDecimal maximum) {return maximum.signum()==0 ? 0 : revenue.multiply(BigDecimal.valueOf(100)).divide(maximum,0,RoundingMode.HALF_UP).intValue();}
    }
    public record TopProduct(String sku,String name,long quantity,BigDecimal revenue) {}
    public record Report(LocalDate from,LocalDate to,BigDecimal revenue,long orderCount,long units,
                         BigDecimal average,BigDecimal maxDaily,List<Daily> daily,List<TopProduct> top,long undatedCompleted) {}
    public Report report(LocalDate from,LocalDate to) {
        if(from==null || to==null || from.isAfter(to)) throw new BusinessException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        if(ChronoUnit.DAYS.between(from,to)>365 || to.getYear()>9998 || from.getYear()<1970)
            throw new BusinessException("Chọn khoảng tối đa 366 ngày, từ năm 1970 đến 9998.");
        var start=from.atStartOfDay();var end=to.plusDays(1).atStartOfDay();
        var data=jdbc.query("SELECT DATE(completed_at) AS sales_day,COUNT(*) AS orders,COALESCE(SUM(total-shipping_fee),0) AS revenue FROM customer_orders WHERE status='COMPLETED' AND completed_at>=? AND completed_at<? GROUP BY DATE(completed_at) ORDER BY sales_day",
            (rs,n) -> new Daily(rs.getDate("sales_day").toLocalDate(),rs.getLong("orders"),rs.getBigDecimal("revenue")),start,end);
        Map<LocalDate,Daily> indexed=new HashMap<>();data.forEach(d -> indexed.put(d.day(),d));
        var daily=new ArrayList<Daily>();
        for(var d=from;!d.isAfter(to);d=d.plusDays(1)) daily.add(indexed.getOrDefault(d,new Daily(d,0,BigDecimal.ZERO)));
        BigDecimal revenue=data.stream().map(Daily::revenue).reduce(BigDecimal.ZERO,BigDecimal::add);
        long count=data.stream().mapToLong(Daily::orderCount).sum();
        long units=jdbc.queryForObject("SELECT COALESCE(SUM(i.quantity),0) FROM order_items i JOIN customer_orders o ON o.id=i.order_id WHERE o.status='COMPLETED' AND o.completed_at>=? AND o.completed_at<?",Long.class,start,end);
        var top=jdbc.query("SELECT latest.sku,latest.product_name,SUM(i.quantity) AS quantity,SUM(i.quantity*i.unit_price) AS revenue FROM order_items i JOIN customer_orders o ON o.id=i.order_id JOIN order_items latest ON latest.id=(SELECT MAX(i2.id) FROM order_items i2 JOIN customer_orders o2 ON o2.id=i2.order_id WHERE i2.product_id=i.product_id AND o2.status='COMPLETED' AND o2.completed_at>=? AND o2.completed_at<?) WHERE o.status='COMPLETED' AND o.completed_at>=? AND o.completed_at<? GROUP BY i.product_id,latest.sku,latest.product_name ORDER BY revenue DESC,i.product_id ASC LIMIT 10",
            (rs,n) -> new TopProduct(rs.getString("sku"),rs.getString("product_name"),rs.getLong("quantity"),rs.getBigDecimal("revenue")),start,end,start,end);
        long undated=jdbc.queryForObject("SELECT COUNT(*) FROM customer_orders WHERE status='COMPLETED' AND completed_at IS NULL",Long.class);
        return new Report(from,to,revenue,count,units,count==0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(count),0,RoundingMode.HALF_UP),
            daily.stream().map(Daily::revenue).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO),daily,top,undated);
    }
}
