package vn.sunicoffee.web;

import jakarta.servlet.http.HttpSession;
import java.util.*;

/** Bounded per-session token set: independent tabs plus safe replay of submitted orders. */
final class CheckoutTokens {
    private CheckoutTokens() {}
    @SuppressWarnings("unchecked")
    static String issue(HttpSession session) {
        synchronized(session) {
            var existing=(Set<String>)session.getAttribute("checkoutTokens");
            var tokens=existing==null ? new LinkedHashSet<String>() : new LinkedHashSet<>(existing);
            String token=UUID.randomUUID().toString(); tokens.add(token);
            while(tokens.size()>20) tokens.remove(tokens.iterator().next());
            session.setAttribute("checkoutTokens",tokens);
            session.setAttribute("checkoutToken",token);
            return token;
        }
    }
    static boolean contains(HttpSession session,String token) {
        synchronized(session) {
            return session.getAttribute("checkoutTokens") instanceof Set<?> tokens && token!=null && tokens.contains(token);
        }
    }
}
