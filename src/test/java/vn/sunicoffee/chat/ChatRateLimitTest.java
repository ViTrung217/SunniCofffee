package vn.sunicoffee.chat;

import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ChatRateLimitTest {
    @Test void limitsPerPeerAndOverallRequestVolume() {
        var limiter = new ChatRateLimit(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int n=0; n<10; n++) assertThat(limiter.allow("peer")).isTrue();
        assertThat(limiter.allow("peer")).isFalse();
        for (int n=0; n<50; n++) assertThat(limiter.allow("peer-"+n)).isTrue();
        assertThat(limiter.allow("another")).isFalse();
    }
}
