package univ.sr2.flopbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import univ.sr2.flopbox.dto.FtpResponse;
import univ.sr2.flopbox.dto.RenameRequest;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.service.FileService;
import univ.sr2.flopbox.service.ServerService;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@ActiveProfiles("test")
@WebMvcTest(FileController.class)
@DisplayName("Tests contrôleur — FileController")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private ServerService serverService;

    private final String HOST = "ftp.test.com";

    @Test
    @DisplayName("GET .../files — Télécharge un fichier avec succès")
    void downloadFile_success() throws Exception {
        Server server = new Server(1, HOST, "Alias", 21);
        when(serverService.getServerByHost(HOST)).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mock(FTPClient.class));
        when(fileService.downloadFile(any(), eq("test.txt"))).thenReturn(new ByteArrayInputStream("content".getBytes()));

        mockMvc.perform(get("/api/v1/servers/" + HOST + "/files")
                        .param("path", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachement\"; filename=\"test.txt\""));
    }

    @Test
    @DisplayName("POST .../files — Upload d'un fichier avec succès")
    void uploadFile_success() throws Exception {
        Server server = new Server(1, HOST, "Alias", 21);
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());

        when(serverService.getServerByHost(HOST)).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mock(FTPClient.class));
        when(fileService.uploadFile(any(), anyString(), any(), anyBoolean()))
                .thenReturn(new FtpResponse<>(true, "Success", 226, null));

        mockMvc.perform(multipart("/api/v1/servers/" + HOST + "/files")
                        .file(file)
                        .param("path", "/remote/path")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Upload réussi avec succès"));
    }

    @Test
    @DisplayName("PATCH .../files — Renomme un fichier avec succès")
    void renameFile_success() throws Exception {
        Server server = new Server(1, HOST, "Alias", 21);
        RenameRequest request = new RenameRequest("old.txt", "new.txt");

        when(serverService.getServerByHost(HOST)).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mock(FTPClient.class));
        when(fileService.rename(any(), eq("old.txt"), eq("new.txt")))
                .thenReturn(new FtpResponse<>(true, "Renamed", 250, null));

        mockMvc.perform(patch("/api/v1/servers/" + HOST + "/files")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Fichier renommé avec succès"));
    }

    @Test
    @DisplayName("DELETE .../files — Supprime un fichier avec succès")
    void deleteFile_success() throws Exception {
        Server server = new Server(1, HOST, "Alias", 21);
        when(serverService.getServerByHost(HOST)).thenReturn(server);
        when(serverService.connect(any(), anyString(), anyString())).thenReturn(mock(FTPClient.class));
        when(fileService.delete(any(), eq("file.txt")))
                .thenReturn(new FtpResponse<>(true, "Deleted", 250, null));

        mockMvc.perform(delete("/api/v1/servers/" + HOST + "/files")
                        .with(csrf())
                        .param("path", "file.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}