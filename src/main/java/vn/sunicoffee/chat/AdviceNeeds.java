package vn.sunicoffee.chat;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/** Extract only explicit customer preferences; newer statements replace older ones. */
public record AdviceNeeds(String brewing, String people, BigDecimal budgetVnd, String beans) {
    public static AdviceNeeds from(List<ChatConversation.Turn> history, String question) {
        var questions = new ArrayList<>(history.stream().map(ChatConversation.Turn::question).toList());
        questions.add(question);
        String brewing = "", people = "", beans = "";
        BigDecimal budget = null;
        for (String text : questions) {
            String normalized = KnowledgeBase.normalize(text);
            var method = Pattern.compile("\\b(pha may|espresso|pha phin|phin|french press|aeropress|pour over|v60)\\b").matcher(normalized);
            while (method.find()) brewing = method.group();
            var count = Pattern.compile("\\b(\\d{1,3})\\s*nguoi\\b").matcher(normalized);
            while (count.find()) people = count.group(1);
            var money = Pattern.compile("(?iu)(?<![\\d.,])(\\d{1,9}(?:[.,]\\d{1,2})?)\\s*(triệu|tr|nghìn|ngàn|k|vnd|đồng)\\b").matcher(text);
            while (money.find()) {
                String unit = KnowledgeBase.normalize(money.group(2));
                int multiplier = Set.of("trieu", "tr").contains(unit) ? 1_000_000 : Set.of("nghin", "ngan", "k").contains(unit) ? 1000 : 1;
                budget = new BigDecimal(money.group(1).replace(',', '.')).multiply(BigDecimal.valueOf(multiplier));
            }
            var bean = Pattern.compile("\\b(arabica|robusta)\\b").matcher(normalized);
            while (bean.find()) beans = bean.group();
        }
        return new AdviceNeeds(brewing, people, budget, beans);
    }
}
