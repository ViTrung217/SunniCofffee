package vn.sunicoffee.chat;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/** Conservative hints, not an intent classifier. The model also sees the original conversation. */
public record AdviceNeeds(String brewing, String people, BigDecimal budgetVnd, String beans) {
    static final Pattern MONEY = Pattern.compile("(?iu)(?<![\\d.,])(\\d{1,3}(?:[., ]\\d{3})+|\\d{1,9}(?:[.,]\\d{1,2})?)\\s*(triệu|tr|nghìn|ngàn|k|vnd|đồng|đ)(?!\\p{L})");
    public static boolean knowledgeQuestion(String text) {
        return Pattern.compile("\\b(tai sao|vi sao|khac|so sanh|the nao|la gi|cach |bao quan|giai thich|huong dan|co nen|co phai)\\b")
            .matcher(KnowledgeBase.normalize(text)).find();
    }
    public static boolean preferenceStatement(String text) {
        String n = KnowledgeBase.normalize(text);
        return !text.contains("?") && !knowledgeQuestion(text)
            && !Pattern.compile("\\b(khong|chang|chua|dung|thoi|bo)\\b").matcher(n).find()
            && n.replaceAll("\\b(toi|minh|muon|mua|may|pha|cho|nguoi|ngan|sach|trieu|tr|nghin|ngan|k|vnd|dong|d|arabica|robusta|hat|ca|phe|giam|tang|con|doi|sang|la|va|phin|espresso|french|press|aeropress|pour|over|v60)\\b|[0-9]+|\\s+", "").isEmpty();
    }
    private static boolean negated(String text, int start) {
        // Limit scope to the current clause, allowing a positive correction after a conjunction.
        String prefix = text.substring(0, start).replaceAll(".*\\b(nhung|ma|thay vao do|chuyen sang)\\b", "");
        return Pattern.compile("\\b(khong|chang|chua|dung|bo)\\b(?:\\s+\\w+){0,5}\\s*$").matcher(prefix).find();
    }
    public static AdviceNeeds from(List<ChatConversation.Turn> history, String question) {
        var questions = new ArrayList<>(history.stream().map(ChatConversation.Turn::question).toList());
        questions.add(question);
        String brewing = "", people = "", beans = "";
        BigDecimal budget = null;
        for (String text : questions) {
            if (knowledgeQuestion(text)) continue; // Mentioning a bean/method in a comparison is not choosing it.
            String normalized = KnowledgeBase.normalize(text);
            var method = Pattern.compile("\\b(pha may|may pha|espresso|pha phin|phin|french press|aeropress|pour over|v60)\\b").matcher(normalized);
            while (method.find()) {
                String value = method.group().equals("may pha") ? "pha may" : method.group();
                if (negated(normalized, method.start())) { if (brewing.equals(value)) brewing = ""; }
                else brewing = value;
            }
            var count = Pattern.compile("\\b(\\d{1,3})\\s*nguoi\\b").matcher(normalized);
            while (count.find()) if (!negated(normalized, count.start())) people = count.group(1);
            var money = MONEY.matcher(text);
            while (money.find()) {
                String unit = KnowledgeBase.normalize(money.group(2));
                int multiplier = Set.of("trieu", "tr").contains(unit) ? 1_000_000 : Set.of("nghin", "ngan", "k").contains(unit) ? 1000 : 1;
                String number = money.group(1);
                if (number.matches("\\d{1,3}(?:[., ]\\d{3})+")) number = number.replaceAll("[., ]", "");
                else number = number.replace(',', '.');
                budget = new BigDecimal(number).multiply(BigDecimal.valueOf(multiplier));
            }
            var bean = Pattern.compile("\\b(arabica|robusta)\\b").matcher(normalized);
            while (bean.find()) {
                if (negated(normalized, bean.start())) { if (beans.equals(bean.group())) beans = ""; }
                else beans = bean.group();
            }
        }
        return new AdviceNeeds(brewing, people, budget, beans);
    }
}
