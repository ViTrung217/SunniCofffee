package vn.sunicoffee.chat;

import java.util.regex.Pattern;

final class ChatPrivacy {
    private ChatPrivacy() {}
    static String redact(String text) {
        // Currency spans are preserved; other digit sequences retain the existing contact redaction.
        var money = AdviceNeeds.MONEY.matcher(text);
        var result = new StringBuilder();
        int end = 0;
        while (money.find()) {
            result.append(contacts(text.substring(end, money.start()))).append(money.group());
            end = money.end();
        }
        return result.append(contacts(text.substring(end))).toString();
    }
    private static String contacts(String text) {
        return text.replaceAll("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}", "[email đã ẩn]")
            .replaceAll("(?<!\\d)\\+?\\d[\\d .-]{7,}\\d(?!\\d)", "[số liên hệ đã ẩn]");
    }
    static boolean publicResearch(String query) {
        String n = KnowledgeBase.normalize(query);
        return query.length() >= 5 && query.length() <= 400 && redact(query).equals(query)
            && !Pattern.compile("\\b(sunicoffee|suni|sku|demo|don hang|van don|tai khoan|dia chi|mat khau|api key|ton kho|cua hang|bao hanh|doi tra|hoan tien|chinh sach)\\b").matcher(n).find();
    }
}
