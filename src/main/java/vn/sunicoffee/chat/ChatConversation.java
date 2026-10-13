package vn.sunicoffee.chat;

import java.util.*;

/** Server-owned, bounded public-knowledge history. Never stores order/database replies. */
public final class ChatConversation {
    public record Turn(String question, String answer) {}
    private final ArrayDeque<Turn> turns = new ArrayDeque<>();
    public List<Turn> history() { return List.copyOf(turns); }
    public void remember(String question, String answer) {
        turns.addLast(new Turn(question, answer));
        while (turns.size() > 6) turns.removeFirst();
    }
    public void clear() { turns.clear(); }
}
