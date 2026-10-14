package vn.sunicoffee.payment;
import org.springframework.stereotype.Service;
@Service
public class PaymentService {
    private final PaymentStore store; private final SepayClient client;
    public PaymentService(PaymentStore store,SepayClient client) {this.store=store;this.client=client;}
    // No database transaction/row lock remains open during the network request.
    public PaymentStore.View check(long id,String email,boolean admin) {
        var query=store.prepareCheck(id,email,admin);
        if(query!=null) store.apply(id,client.transfers(query));
        return store.view(id,email,admin);
    }
}
