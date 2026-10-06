package vn.sunicoffee;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import vn.sunicoffee.config.LoginThrottleFilter;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class LoginThrottleTest {
    @Test void excessiveAttemptsAreLimitedPerIpAndGetStillWorks() throws Exception {
        var filter=new LoginThrottleFilter(2);
        for(int i=0;i<3;i++) {
            var request=new MockHttpServletRequest("POST","/login");request.setServletPath("/login");request.setRemoteAddr("127.0.0.1");
            var response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(i<2 ? 200 : 429);
            if(i==2) assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        }
        var get=new MockHttpServletRequest("GET","/login");get.setServletPath("/login");
        var response=new MockHttpServletResponse();filter.doFilter(get,response,new MockFilterChain());assertThat(response.getStatus()).isEqualTo(200);
        var other=new MockHttpServletRequest("POST","/login");other.setServletPath("/login");other.setRemoteAddr("127.0.0.2");
        response=new MockHttpServletResponse();filter.doFilter(other,response,new MockFilterChain());assertThat(response.getStatus()).isEqualTo(200);
    }
    @Test void windowExpiresWithoutSleeping() throws Exception {
        var now=new java.util.concurrent.atomic.AtomicLong(0);
        Clock clock=new Clock() {
            public ZoneId getZone(){return ZoneOffset.UTC;}
            public Clock withZone(ZoneId zone){return this;}
            public Instant instant(){return Instant.ofEpochMilli(now.get());}
        };
        var filter=new LoginThrottleFilter(1,clock);
        for(int expected:new int[]{200,429,200}) {
            if(expected==200 && now.get()==1) now.set(60000);
            var request=new MockHttpServletRequest("POST","/login");request.setServletPath("/login");
            var response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(expected);
            if(expected==429) now.set(1);
        }
    }
}
