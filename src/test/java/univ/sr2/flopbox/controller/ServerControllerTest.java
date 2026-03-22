package univ.sr2.flopbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import univ.sr2.flopbox.dto.*;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.service.ServerService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@ActiveProfiles("test")
@WebMvcTest(ServerController.class)
@DisplayName("Tests contrôleur — ServerController")
class ServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServerService serverService;

    // POST /api/v1/servers — addServer

    @Test
    @DisplayName("POST /api/v1/servers — crée un serveur et retourne 201")
    void addServer_retourne_201() throws Exception {
        ServerRequest request = new ServerRequest("MonServeur", "ftp.example.com", 21);
        Server saved = new Server(1, "ftp.example.com", "MonServeur", 21);

        when(serverService.addServer(any(ServerRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/servers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Serveur créé avec succès"))
                .andExpect(jsonPath("$.data.host").value("ftp.example.com"))
                .andExpect(jsonPath("$.data.alias").value("MonServeur"))
                .andExpect(jsonPath("$.data.port").value(21));
    }

    @Test
    @DisplayName("POST /api/v1/servers — retourne 400 si l'hôte est déjà enregistré")
    void addServer_retourne_400_si_doublon() throws Exception {
        ServerRequest request = new ServerRequest("Doublon", "ftp.example.com", 21);

        when(serverService.addServer(any(ServerRequest.class)))
                .thenThrow(new RuntimeException("Un serveur avec cet hôte existe déjà."));

        mockMvc.perform(post("/api/v1/servers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("existe déjà")));
    }

    // GET /api/v1/servers — getServeur

    @Test
    @DisplayName("GET /api/v1/servers — retourne la liste des serveurs avec 200")
    void getServeur_retourne_200() throws Exception {
        List<Server> servers = List.of(
                new Server(1, "ftp.a.com", "A", 21),
                new Server(2, "ftp.b.com", "B", 2121)
        );

        when(serverService.getServer()).thenReturn(servers);

        mockMvc.perform(get("/api/v1/servers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Liste des serveurs recupérée avec succés"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].host").value("ftp.a.com"))
                .andExpect(jsonPath("$.data[1].port").value(2121));
    }

    @Test
    @DisplayName("GET /api/v1/servers — retourne une liste vide si aucun serveur")
    void getServeur_liste_vide() throws Exception {
        when(serverService.getServer()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/servers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // DELETE /api/v1/servers — deleteServer

    @Test
    @DisplayName("DELETE /api/v1/servers — supprime un serveur et retourne 200")
    void deleteServer_retourne_200() throws Exception {
        DeleteServerRequest deleteRequest = new DeleteServerRequest("ftp.example.com");
        Server deleted = new Server(1, "ftp.example.com", "Ex", 21);

        when(serverService.deleteServer(any(DeleteServerRequest.class))).thenReturn(deleted);

        mockMvc.perform(delete("/api/v1/servers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Serveur supprimé avec succès"))
                .andExpect(jsonPath("$.data.host").value("ftp.example.com"));
    }

    @Test
    @DisplayName("DELETE /api/v1/servers — retourne 404 si serveur introuvable")
    void deleteServer_retourne_404() throws Exception {
        DeleteServerRequest deleteRequest = new DeleteServerRequest("inconnu.com");

        when(serverService.deleteServer(any(DeleteServerRequest.class)))
                .thenThrow(new RuntimeException("Serveur non trouvé avec l'hôte : inconnu.com"));

        mockMvc.perform(delete("/api/v1/servers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("non trouvé")));
    }

    // PUT /api/v1/servers/{host} — updateServer

    @Test
    @DisplayName("PUT /api/v1/servers/{host} — met à jour un serveur et retourne 200")
    void updateServer_retourne_200() throws Exception {
        ServerRequest updateRequest = new ServerRequest("NouvelAlias", "ftp.new.com", 2121);
        Server updated = new Server(1, "ftp.new.com", "NouvelAlias", 2121);

        when(serverService.updateServer(eq("ftp.old.com"), any(ServerRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/servers/ftp.old.com")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Serveur mis à jour avec succès"))
                .andExpect(jsonPath("$.data.host").value("ftp.new.com"))
                .andExpect(jsonPath("$.data.alias").value("NouvelAlias"))
                .andExpect(jsonPath("$.data.port").value(2121));
    }

    @Test
    @DisplayName("PUT /api/v1/servers/{host} — retourne 404 si serveur introuvable")
    void updateServer_retourne_404() throws Exception {
        ServerRequest updateRequest = new ServerRequest("Alias", "ftp.new.com", 21);

        when(serverService.updateServer(anyString(), any(ServerRequest.class)))
                .thenThrow(new RuntimeException("Serveur non trouvé avec l'hôte : inconnu.com"));

        mockMvc.perform(put("/api/v1/servers/inconnu.com")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // GET /api/v1/servers/{host}/search — searchFile

    @Test
    @DisplayName("GET /api/v1/servers/{host}/search — retourne les résultats trouvés")
    void searchFile_retourne_resultats() throws Exception {
        Server server = new Server(1, "ftp.a.com", "A", 21);
        FtpItem item = new FtpItem("/readme.txt", "readme.txt", Type.FILE, 100, "now");
        FTPClient mockFtpClient = mock(FTPClient.class);

        when(serverService.getServerByHost("ftp.a.com")).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mockFtpClient);
        when(serverService.searchFile(any(FTPClient.class), eq("readme"))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/servers/ftp.a.com/search")
                        .param("searchQuery", "readme")
                        .header("X-FTP-Username", "anonymous")
                        .header("X-FTP-Password", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].ftpItem.name").value("readme.txt"))
                .andExpect(jsonPath("$.data[0].serverRequest.host").value("ftp.a.com"));
    }

    @Test
    @DisplayName("GET /api/v1/servers/{host}/search — retourne liste vide si rien trouvé")
    void searchFile_retourne_liste_vide() throws Exception {
        Server server = new Server(1, "ftp.a.com", "A", 21);

        when(serverService.getServerByHost("ftp.a.com")).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mock(FTPClient.class));
        when(serverService.searchFile(any(FTPClient.class), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/servers/ftp.a.com/search")
                        .param("searchQuery", "inexistant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/servers/{host}/search — retourne 500 si connexion FTP échoue")
    void searchFile_retourne_500_si_connexion_echoue() throws Exception {
        Server server = new Server(1, "ftp.a.com", "A", 21);

        when(serverService.getServerByHost("ftp.a.com")).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString()))
                .thenThrow(new IOException("Connexion refusée"));

        mockMvc.perform(get("/api/v1/servers/ftp.a.com/search")
                        .param("searchQuery", "readme"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Impossible de joindre le serveur")));
    }

    @Test
    @DisplayName("GET /api/v1/servers/{host}/search — déconnecte toujours dans le finally")
    void searchFile_deconnecte_toujours() throws Exception {
        Server server = new Server(1, "ftp.a.com", "A", 21);
        FTPClient mockFtpClient = mock(FTPClient.class);

        when(serverService.getServerByHost("ftp.a.com")).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mockFtpClient);
        when(serverService.searchFile(any(FTPClient.class), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/servers/ftp.a.com/search")
                .param("searchQuery", "test"));

        verify(serverService).disconnect(mockFtpClient);
    }

    // POST /api/v1/servers/search — searchGlobal

    @Test
    @DisplayName("POST /api/v1/servers/search — recherche globale retourne 200 avec résultats fusionnés")
    void searchGlobal_retourne_200() throws Exception {
        FtpItem item = new FtpItem("/readme.txt", "readme.txt", Type.FILE, 100, "now");
        ServerRequest serverReq = new ServerRequest("A", "ftp.a.com", 21);
        SearchResponse sr = new SearchResponse(item, serverReq);

        GlobalSearchRequest body = new GlobalSearchRequest(
                "readme",
                Map.of("s1", new ServerCredentials("ftp.a.com", "user", "pass"))
        );

        when(serverService.searchGlobal(eq("readme"), anyMap())).thenReturn(List.of(sr));

        mockMvc.perform(post("/api/v1/servers/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Recherche globale effectuée avec succès"))
                .andExpect(jsonPath("$.data[0].ftpItem.name").value("readme.txt"))
                .andExpect(jsonPath("$.data[0].serverRequest.host").value("ftp.a.com"));
    }

    @Test
    @DisplayName("POST /api/v1/servers/search — retourne 200 avec liste vide si rien trouvé")
    void searchGlobal_retourne_liste_vide() throws Exception {
        GlobalSearchRequest body = new GlobalSearchRequest("inexistant", Map.of());

        when(serverService.searchGlobal(anyString(), anyMap())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/servers/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/servers/search — retourne 500 si une exception critique survient")
    void searchGlobal_retourne_500_si_exception() throws Exception {
        GlobalSearchRequest body = new GlobalSearchRequest(
                "readme",
                Map.of("s1", new ServerCredentials("ftp.a.com", "user", "pass"))
        );

        when(serverService.searchGlobal(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Erreur critique"));

        mockMvc.perform(post("/api/v1/servers/search")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }
}