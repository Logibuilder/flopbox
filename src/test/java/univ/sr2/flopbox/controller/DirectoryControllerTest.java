package univ.sr2.flopbox.controller;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import univ.sr2.flopbox.dto.FtpItem;
import univ.sr2.flopbox.dto.FtpResponse;
import univ.sr2.flopbox.dto.Type;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.service.DirectoryService;
import univ.sr2.flopbox.service.ServerService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@ActiveProfiles("test")
@WebMvcTest(DirectoryController.class)
@DisplayName("Tests contrôleur — DirectoryController")
class DirectoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DirectoryService directoryService;

    @MockitoBean
    private ServerService serverService;

    private final String HOST = "ftp.test.com";

    @Test
    @DisplayName("GET .../directories — Liste le contenu d'un dossier")
    void listDirectory_success() throws Exception {
        Server server = new Server(1, HOST, "Alias", 21);
        List<FtpItem> items = List.of(new FtpItem("/home/dir", "dir", Type.DIRECTORY, 0, "date"));

        when(serverService.getServerByHost(HOST)).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mock(FTPClient.class));
        when(directoryService.listDirectory(any(), eq("/home"))).thenReturn(items);

        mockMvc.perform(get("/api/v1/servers/" + HOST + "/directories")
                        .param("path", "/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("dir"))
                .andExpect(jsonPath("$.data[0].type").value("DIRECTORY"));

        verify(serverService).disconnect(any());
    }


    @DisplayName("POST .../directories — Crée un dossier")
    @Test
    void makeDirectory_success() throws Exception {
        Server server = new Server(1, HOST, "Alias", 21);
        when(serverService.getServerByHost(HOST)).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mock(FTPClient.class));
        when(directoryService.makeDirectory(any(), eq("/newDir")))
                .thenReturn(new FtpResponse<>(true, "Created", 257, null));

        mockMvc.perform(post("/api/v1/servers/" + HOST + "/directories")
                        .with(csrf())
                        .param("path", "/newDir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data").value("/newDir"));
    }

    @Test
    @DisplayName("DELETE .../directories — Supprime un dossier")
    void deleteDirectory_success() throws Exception {
        Server server = new Server(1, HOST, "Alias", 21);
        when(serverService.getServerByHost(HOST)).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mock(FTPClient.class));
        when(directoryService.delete(any(), eq("/oldDir")))
                .thenReturn(new FtpResponse<>(true, "Removed", 250, null));

        mockMvc.perform(delete("/api/v1/servers/" + HOST + "/directories")
                        .with(csrf())
                        .param("path", "/oldDir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Suppréssion réussie"));
    }

    @Test
    @DisplayName("GET .../directories — Retourne 403 si erreur FTP (IOException)")
    void listDirectory_error_ftp() throws Exception {
        Server server = new Server(1, HOST, "Alias", 21);
        when(serverService.getServerByHost(HOST)).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenThrow(new java.io.IOException("Auth failed"));

        mockMvc.perform(get("/api/v1/servers/" + HOST + "/directories")
                        .param("path", "/"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}