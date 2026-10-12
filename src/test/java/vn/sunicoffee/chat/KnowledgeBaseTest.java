package vn.sunicoffee.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class KnowledgeBaseTest {
    @Test void loadsOnlyPublicSourcesAndFindsNamedEquipmentWithUnaccentedVietnamese() throws Exception {
        var knowledge = new KnowledgeBase(new ObjectMapper());
        var matches = knowledge.search("Kich thuoc va khoi luong Picopresso");
        assertThat(matches).extracting(KnowledgeBase.Entry::id).contains("SP04").doesNotContain("Q01");
        assertThat(knowledge.citations(java.util.List.of("SP04"))).singleElement()
            .satisfies(source -> assertThat(source.url()).isEqualTo("https://www.wacaco.com/products/picopresso"));
        assertThat(knowledge.citations(java.util.List.of("NV25"))).allSatisfy(s -> assertThat(s.url()).startsWith("/chat/sources/"));
    }
}
