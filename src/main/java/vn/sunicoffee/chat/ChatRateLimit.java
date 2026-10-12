package vn.sunicoffee.chat;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Bounded, single-process fixed window. Identity uses the socket peer, not client headers. */
@Component
public class ChatRateLimit {
    private final Clock clock;
    private long minute = -1;
    private int total;
    private final Map<String, Integer> counts = new HashMap<>();
    public ChatRateLimit() { this(Clock.systemUTC()); }
    ChatRateLimit(Clock clock) { this.clock = clock; }
    public synchronized boolean allow(String peer) {
        long now = clock.millis() / 60_000;
        if (now != minute) { counts.clear(); total = 0; minute = now; }
        int count = counts.getOrDefault(peer, 0);
        if (total >= 60 || count >= 10) return false;
        counts.put(peer, count + 1); total++;
        return true;
    }
}
