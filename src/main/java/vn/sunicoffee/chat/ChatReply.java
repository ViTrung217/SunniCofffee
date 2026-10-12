package vn.sunicoffee.chat;

import java.util.List;

public record ChatReply(String status, String answer, List<Citation> sources) {
    public record Citation(String id, String title, String url) {}
    public static ChatReply of(String status, String answer) {
        return new ChatReply(status, answer, List.of());
    }
}
