package vn.sunicoffee.chat;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AdviceNeedsTest {
    @Test void shortBeanReplyKeepsEarlierNeedsAndBudgetCorrectionWins() {
        var history = List.of(new ChatConversation.Turn("pha máy, 2 người, 2 triệu", ""));
        var needs = AdviceNeeds.from(history, "arabica");
        assertThat(needs.brewing()).isEqualTo("pha may");
        assertThat(needs.people()).isEqualTo("2");
        assertThat(needs.budgetVnd()).isEqualByComparingTo("2000000");
        assertThat(needs.beans()).isEqualTo("arabica");
        assertThat(AdviceNeeds.from(history, "Giảm còn 1,5 triệu").budgetVnd()).isEqualByComparingTo("1500000");
        assertThat(AdviceNeeds.from(List.of(), "arabica").budgetVnd()).isNull();
    }
}
