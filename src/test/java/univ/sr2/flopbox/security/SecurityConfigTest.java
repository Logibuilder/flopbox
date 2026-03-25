package univ.sr2.flopbox.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Test d'intégration -Security config")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;


    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Accès autorisé sans token pour les endpoints d'authentification")
    void authGetEnpoint_AreMethodeNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login")).andExpect(status().isMethodNotAllowed());
    }
    @Test
    @DisplayName("Accès refusé (403) sans token pour les servers")
    void serverEndPoints_AreForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/servers")).andExpect(status().isForbidden());

    }

    @Test
    @DisplayName("Accès autorisé pour swagger UI")
    void swagger_ArePublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

}