package univ.sr2.flopbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import univ.sr2.flopbox.dto.LoginRequest;
import univ.sr2.flopbox.dto.LoginResponse;
import univ.sr2.flopbox.dto.UserRequest;
import univ.sr2.flopbox.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests — AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Inscription réussie")
    void register_Success() throws Exception {
        UserRequest request = new UserRequest("test@mail.com", "Test", "pass123");
        when(userService.register(any())).thenReturn(request);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Utilisateur créé avec succès"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("Connexion réussie — Retourne les tokens")
    void login_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@mail.com", "pass123");
        LoginResponse loginResponse = new LoginResponse("test@mail.com", "Test", "access-token", "refresh-token");

        when(userService.login(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @WithMockUser(username = "test@mail.com") // Simule un utilisateur connecté
    @DisplayName("Déconnexion réussie pour utilisateur authentifié")
    void logout_Success() throws Exception {
        UserRequest userRequest = new UserRequest("test@mail.com", "Test", null);
        when(userService.logout()).thenReturn(userRequest);

        mockMvc.perform(get("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Déconnection réussie"));
    }

    @Test
    @DisplayName("Déconnexion refusée (403) si non connecté")
    void logout_Forbidden() throws Exception {
        // Sans @WithMockUser, SecurityConfig rejette la requête car .anyRequest().authenticated()
        mockMvc.perform(get("/api/v1/auth/logout"))
                .andExpect(status().isForbidden());
    }
}