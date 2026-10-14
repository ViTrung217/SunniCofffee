package vn.sunicoffee.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import vn.sunicoffee.domain.CustomerOrder;
import vn.sunicoffee.service.BusinessException;
import java.util.UUID;

@ConfigurationProperties(prefix="app.sepay")
public record SepaySettings(boolean enabled, boolean sandbox, String apiToken, String bankAccountId,
        String accountNumber, String qrAccount, String bank, String holder, String descriptionPrefix, String va) {
    @Configuration @EnableConfigurationProperties(SepaySettings.class)
    static class Config {}
    public boolean ready() {
        return enabled && text(apiToken,512) && bankAccountId!=null && bankAccountId.matches("[a-fA-F0-9-]{36}")
            && accountNumber!=null && accountNumber.matches("[a-zA-Z0-9]{1,64}")
            && qrAccount!=null && qrAccount.matches("[a-zA-Z0-9]{1,64}")
            && text(bank,40) && text(holder,100) && descriptionPrefix!=null && descriptionPrefix.matches("[a-zA-Z0-9 ]{0,40}")
            && va!=null && va.matches("[a-zA-Z0-9]{0,64}");
    }
    private boolean text(String value,int max) { return value!=null && !value.isBlank() && value.length()<=max; }
    public void prepare(CustomerOrder order) {
        if(!ready()) throw new BusinessException("Thanh toán QR chưa được cấu hình. Vui lòng chọn COD hoặc liên hệ cửa hàng.");
        order.setPaymentCode("SC"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase(java.util.Locale.ROOT));
        order.setPaymentSandbox(sandbox);
        order.setSepayBankAccountId(bankAccountId); order.setSepayAccountNumber(accountNumber);
        order.setSepayQrAccount(qrAccount); order.setSepayBank(bank); order.setSepayHolder(holder);
        order.setSepayPrefix(descriptionPrefix.strip()); order.setSepayVa(va);
    }
}
