package vn.sunicoffee.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.filter.OncePerRequestFilter;

/** Single-instance, bounded IP rate limiter. Never trusts forwarded headers from clients. */
public class LoginThrottleFilter extends OncePerRequestFilter {
    private record Window(long expires,int count) {}
    private final Map<String,Window> windows=new HashMap<>();
    private final int limit;
    private final Clock clock;
    public LoginThrottleFilter(int limit) {this(limit,Clock.systemUTC());}
    public LoginThrottleFilter(int limit,Clock clock) {
        if(limit<1) throw new IllegalArgumentException("Login limit must be positive");
        this.limit=limit;this.clock=clock;
    }
    private synchronized boolean allow(String address) {
        long now=clock.millis();
        windows.entrySet().removeIf(e -> e.getValue().expires()<=now);
        Window window=windows.get(address);
        if(window==null) {
            if(windows.size()>=10000) return false;
            windows.put(address,new Window(now+60000,1));return true;
        }
        if(window.count()>=limit) return false;
        windows.put(address,new Window(window.expires(),window.count()+1));return true;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if(request.getMethod().equals("POST") && (request.getServletPath().equals("/login") || request.getServletPath().equals("/login/google/link")) && !allow(request.getRemoteAddr())) {
            response.setStatus(429);response.setHeader("Retry-After","60");response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau 60 giây.");return;
        }
        chain.doFilter(request,response);
    }
}
