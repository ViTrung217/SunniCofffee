package vn.sunicoffee.chat;

import java.util.List;

public record ChatReply(String status, String answer, List<Citation> sources, String searchSuggestionsHtml) {
    public ChatReply(String status, String answer, List<Citation> sources) { this(status, answer, sources, null); }
    public record Citation(String id, String title, String url) {}
    public static ChatReply of(String status, String answer) {
        return new ChatReply(status, answer, List.of());
    }
}
