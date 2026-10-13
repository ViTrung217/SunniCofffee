package vn.sunicoffee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties="app.chat.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class ReleaseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired vn.sunicoffee.service.UserService users;
    @Test void healthAndProbesExposeOnlyStatus() throws Exception {
        for (String path : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist()).andExpect(jsonPath("$.details").doesNotExist());
        }
    }
    @Test void evenAdministratorCannotReadActuatorSecretsOrChangeLoggers() throws Exception {
        users.create("Release admin", "release-admin@example.test", "SyntheticPass!2026", vn.sunicoffee.domain.Role.ADMIN);
        mvc.perform(get("/actuator/env").with(user("release-admin@example.test").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/configprops").with(user("release-admin@example.test").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/actuator/loggers/ROOT").with(user("release-admin@example.test").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"configuredLevel\":\"DEBUG\"}")).andExpect(status().isForbidden());
    }
}
