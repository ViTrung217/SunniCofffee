package vn.sunicoffee.chat;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ChatConversationTest {
    @Test void boundsHistoryAndClearsItWithoutChangingExistingSnapshots() {
        var conversation = new ChatConversation();
        conversation.remember("first", "answer");
        var snapshot = conversation.history();
        for (int i = 0; i < 10; i++) conversation.remember("question " + i, "answer");
        assertThat(conversation.history()).hasSize(6);
        assertThat(conversation.history().getFirst().question()).isEqualTo("question 4");
        conversation.clear();
        assertThat(conversation.history()).isEmpty();
        assertThat(snapshot).hasSize(1);
    }
}
