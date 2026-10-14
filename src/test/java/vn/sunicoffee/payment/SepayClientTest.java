package vn.sunicoffee.payment;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class SepayClientTest {
    MockRestServiceServer server; SepayClient client;
    SepaySettings settings(boolean enabled,boolean sandbox) {return new SepaySettings(enabled,sandbox,"test-only-secret","11111111-1111-1111-1111-111111111111","0123456789","0123456789","Vietcombank","TEST ACCOUNT","","");}
    SepayClient.Query query(boolean sandbox) {return new SepayClient.Query("SC01234567890123456789","11111111-1111-1111-1111-111111111111",sandbox);}
    @BeforeEach void setup() {var b=RestClient.builder();server=MockRestServiceServer.bindTo(b).build();client=new SepayClient(settings(true,true),b);}
    @Test void sandboxRequestsAreAuthenticatedFilteredAndParsedWithoutExposingBalance() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://userapi-sandbox.sepay.vn/v2/transactions?")))
            .andExpect(header("Authorization","Bearer test-only-secret"))
            .andExpect(queryParam("q",query(true).code())).andExpect(queryParam("bank_account_id",query(true).bankAccountId()))
            .andRespond(withSuccess("""
                {"status":"success","data":[{"id":"22222222-2222-2222-2222-222222222222","bank_account_id":"11111111-1111-1111-1111-111111111111","account_number":"0123456789","va":null,"transfer_type":"in","amount_in":120000,"amount_out":0,"transaction_content":"SC01234567890123456789","transaction_date":"2026-10-14T12:00:00+07:00","reference_number":"TESTREF","accumulated":9999999}],"meta":{"pagination":{"has_more":false}}}
                """,MediaType.APPLICATION_JSON));
        var rows=client.transfers(query(true));assertThat(rows).hasSize(1);assertThat(rows.getFirst().amountIn()).isEqualByComparingTo("120000");assertThat(rows.getFirst().va()).isEmpty();server.verify();
    }
    @Test void liveAndSandboxCannotBeMixed() {assertThatThrownBy(()->client.transfers(query(false))).isInstanceOf(SepayClient.Unavailable.class);server.verify();}
    @Test void morePagesFailClosedInsteadOfCreditingIncompleteAmount() {
        server.expect(anything()).andRespond(withSuccess("{\"status\":\"success\",\"data\":[],\"meta\":{\"pagination\":{\"has_more\":true}}}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->client.transfers(query(true))).isInstanceOf(SepayClient.Unavailable.class);server.verify();
    }
    @Test void malformedPayloadAndProviderSecretsAreNotExposed() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("test-only-secret bank private data"));
        assertThatThrownBy(()->client.transfers(query(true))).isInstanceOf(SepayClient.Unavailable.class).hasNoCause().hasMessageNotContaining("test-only-secret");server.verify();
    }
    @Test void missingPaginationDoesNotSilentlyTreatDataAsComplete() {
        server.expect(anything()).andRespond(withSuccess("{\"status\":\"success\",\"data\":[]}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->client.transfers(query(true))).isInstanceOf(SepayClient.Unavailable.class);server.verify();
    }
    @Test void disabledConfigurationNeverMakesNetworkRequest() {
        var b=RestClient.builder();var noRequests=MockRestServiceServer.bindTo(b).build();var disabled=new SepayClient(settings(false,true),b);
        assertThatThrownBy(()->disabled.transfers(query(true))).isInstanceOf(SepayClient.Unavailable.class);noRequests.verify();
    }
}
