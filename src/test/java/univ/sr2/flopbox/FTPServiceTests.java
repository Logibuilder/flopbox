package univ.sr2.flopbox;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import univ.sr2.flopbox.dto.FtpItem;
import univ.sr2.flopbox.dto.FtpResponse;
import univ.sr2.flopbox.dto.Type;
import univ.sr2.flopbox.service.FTPService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — FTPService")
class FTPServiceTest {

    @InjectMocks
    private FTPService ftpService;

    @Mock
    private FTPClient ftpClient;

    // ─────────────────────────────────────────────
    // listDirectory
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("listDirectory — retourne les éléments d'un répertoire")
    void listDirectory_retourne_les_elements() throws IOException {
        FTPFile file = new FTPFile();
        file.setName("readme.txt");
        file.setType(FTPFile.FILE_TYPE);
        file.setSize(1024);

        FTPFile dir = new FTPFile();
        dir.setName("documents");
        dir.setType(FTPFile.DIRECTORY_TYPE);

        when(ftpClient.listFiles("/")).thenReturn(new FTPFile[]{file, dir});

        List<FtpItem> items = ftpService.listDirectory(ftpClient, "/");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).name()).isEqualTo("readme.txt");
        assertThat(items.get(0).type()).isEqualTo(Type.FILE);
        assertThat(items.get(1).name()).isEqualTo("documents");
        assertThat(items.get(1).type()).isEqualTo(Type.DIRECTORY);
    }

    @Test
    @DisplayName("listDirectory — retourne une liste vide si le serveur renvoie null")
    void listDirectory_null_retourne_liste_vide() throws IOException {
        when(ftpClient.listFiles(anyString())).thenReturn(null);

        List<FtpItem> items = ftpService.listDirectory(ftpClient, "/vide");

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("listDirectory — le chemin '/' est normalisé en chaîne vide pour le FTP")
    void listDirectory_chemin_racine_normalise() throws IOException {
        when(ftpClient.listFiles("")).thenReturn(new FTPFile[0]);

        List<FtpItem> items = ftpService.listDirectory(ftpClient, "");

        verify(ftpClient).listFiles("");
        assertThat(items).isEmpty();
    }

    // APRÈS (à mettre à la place)
    @Test
    @DisplayName("listDirectory — le chemin est transmis tel quel au serveur FTP")
    void listDirectory_chemin_transmis_tel_quel() throws IOException {
        when(ftpClient.listFiles("/")).thenReturn(new FTPFile[0]);

        ftpService.listDirectory(ftpClient, "/");

        verify(ftpClient).listFiles("/");
    }

    @Test
    @DisplayName("listDirectory — le chemin du fichier est construit correctement")
    void listDirectory_chemin_fichier_correct() throws IOException {
        FTPFile file = new FTPFile();
        file.setName("image.png");
        file.setType(FTPFile.FILE_TYPE);

        when(ftpClient.listFiles("/photos")).thenReturn(new FTPFile[]{file});

        List<FtpItem> items = ftpService.listDirectory(ftpClient, "/photos");

        assertThat(items.get(0).path()).isEqualTo("/photos/image.png");
    }

    // ─────────────────────────────────────────────
    // uploadFile
    // ─────────────────────────────────────────────


    @Test
    @DisplayName("uploadFile — upload réussi retourne FtpResponse succès")
    void uploadFile_succes() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("contenu".getBytes());
        when(ftpClient.listNames(anyString())).thenReturn(null);
        when(ftpClient.storeFile(anyString(), any(InputStream.class))).thenReturn(true);
        when(ftpClient.getReplyCode()).thenReturn(226);

        FtpResponse<Void> response = ftpService.uploadFile(ftpClient, "/test.txt", inputStream, false);

        assertThat(response.succes()).isTrue();
        assertThat(response.code()).isEqualTo(226);
        verify(ftpClient).storeFile(eq("/test.txt"), any(InputStream.class));
    }

    @Test
    @DisplayName("uploadFile — retourne FtpResponse échec si fichier existe et replace=false")
    void uploadFile_echec_si_fichier_existe_sans_replace() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("contenu".getBytes());
        when(ftpClient.listNames(anyString())).thenReturn(new String[]{"/test.txt"});

        FtpResponse<Void> response = ftpService.uploadFile(ftpClient, "/test.txt", inputStream, false);

        assertThat(response.succes()).isFalse();
        assertThat(response.code()).isEqualTo(450);
        assertThat(response.message()).contains("remplacement n'est pas autorisé");
    }

    @Test
    @DisplayName("uploadFile — remplace un fichier existant si replace=true")
    void uploadFile_remplace_si_replace_true() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("contenu".getBytes());
        when(ftpClient.listNames(anyString())).thenReturn(new String[]{"/test.txt"});
        when(ftpClient.storeFile(anyString(), any(InputStream.class))).thenReturn(true);
        when(ftpClient.getReplyCode()).thenReturn(226);

        FtpResponse<Void> response = ftpService.uploadFile(ftpClient, "/test.txt", inputStream, true);

        assertThat(response.succes()).isTrue();
    }

    @Test
    @DisplayName("uploadFile — retourne FtpResponse échec si storeFile retourne false")
    void uploadFile_leve_exception_si_echec_ftp() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("contenu".getBytes());
        when(ftpClient.listNames(anyString())).thenReturn(null);
        when(ftpClient.storeFile(anyString(), any(InputStream.class))).thenReturn(false);
        when(ftpClient.getReplyCode()).thenReturn(550);
        when(ftpClient.getReplyString()).thenReturn("550 Permission denied");

        FtpResponse<Void> response = ftpService.uploadFile(ftpClient, "/test.txt", inputStream, false);

        assertThat(response.succes()).isFalse();
        assertThat(response.code()).isEqualTo(550);
        assertThat(response.message()).contains("Échec de l'upload");
    }

    // ─────────────────────────────────────────────
    // downloadFile
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("downloadFile — retourne un InputStream si le fichier existe")
    void downloadFile_succes() throws IOException {
        InputStream fakeStream = new ByteArrayInputStream("contenu".getBytes());
        // retrieveFileStream() et non retrieveFile()
        when(ftpClient.retrieveFileStream(anyString())).thenReturn(fakeStream);

        InputStream result = ftpService.downloadFile(ftpClient, "/test.txt");

        assertThat(result).isNotNull();
        verify(ftpClient).retrieveFileStream("/test.txt");
    }

    @Test
    @DisplayName("downloadFile — lève une IOException si le fichier est introuvable (stream null)")
    void downloadFile_echec_introuvable() throws IOException {
        // retrieveFileStream() retourne null si le fichier est introuvable
        when(ftpClient.retrieveFileStream(anyString())).thenReturn(null);
        when(ftpClient.getReplyCode()).thenReturn(550);
        when(ftpClient.getReplyString()).thenReturn("550 No such file or directory");

        assertThatThrownBy(() -> ftpService.downloadFile(ftpClient, "/inexistant.txt"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("550");
    }

    // ─────────────────────────────────────────────
    // rename
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("rename — renommage réussi")
    void rename_succes() throws IOException {
        when(ftpClient.rename(anyString(), anyString())).thenReturn(true);

        FtpResponse<Void> response = ftpService.rename(ftpClient, "/ancien.txt", "/nouveau.txt");

        assertThat(response.succes()).isTrue();
        assertThat(response.message()).contains("succès");
    }

    @Test
    @DisplayName("rename — retourne échec si le FTP refuse")
    void rename_echec_ftp() throws IOException {
        when(ftpClient.rename(anyString(), anyString())).thenReturn(false);
        when(ftpClient.getReplyString()).thenReturn("550 File not found");

        FtpResponse<Void> response = ftpService.rename(ftpClient, "/ancien.txt", "/nouveau.txt");

        assertThat(response.succes()).isFalse();
        assertThat(response.message()).contains("550");
    }

    // ─────────────────────────────────────────────
    // deleteFile
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteFile — suppression réussie")
    void deleteFile_succes() throws IOException {
        when(ftpClient.deleteFile(anyString())).thenReturn(true);

        FtpResponse<Void> response = ftpService.deleteFile(ftpClient, "/old.txt");

        assertThat(response.succes()).isTrue();
        assertThat(response.message()).contains("supprimé");
    }

    @Test
    @DisplayName("deleteFile — retourne échec si le FTP refuse")
    void deleteFile_echec_ftp() throws IOException {
        when(ftpClient.deleteFile(anyString())).thenReturn(false);
        when(ftpClient.getReplyString()).thenReturn("550 Permission denied");

        FtpResponse<Void> response = ftpService.deleteFile(ftpClient, "/old.txt");

        assertThat(response.succes()).isFalse();
    }

    // ─────────────────────────────────────────────
    // deleteDirectory
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteDirectory — suppression réussie")
    void deleteDirectory_succes() throws IOException {
        when(ftpClient.removeDirectory(anyString())).thenReturn(true);

        FtpResponse<Void> response = ftpService.deleteDirectory(ftpClient, "/monDossier");

        assertThat(response.succes()).isTrue();
    }

    @Test
    @DisplayName("deleteDirectory — retourne échec si dossier inexistant")
    void deleteDirectory_echec() throws IOException {
        when(ftpClient.removeDirectory(anyString())).thenReturn(false);
        when(ftpClient.getReplyString()).thenReturn("550 Not found");

        FtpResponse<Void> response = ftpService.deleteDirectory(ftpClient, "/fantome");

        assertThat(response.succes()).isFalse();
    }

    // ─────────────────────────────────────────────
    // makeDirectory
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("makeDirectory — création réussie")
    void makeDirectory_succes() throws IOException {
        when(ftpClient.printWorkingDirectory()).thenReturn("/");
        when(ftpClient.changeWorkingDirectory(anyString())).thenReturn(false); // dossier n'existe pas
        when(ftpClient.makeDirectory(anyString())).thenReturn(true);

        FtpResponse<Void> response = ftpService.makeDirectory(ftpClient, "/nouveau");

        assertThat(response.succes()).isTrue();
    }

    @Test
    @DisplayName("makeDirectory — retourne échec si le dossier existe déjà")
    void makeDirectory_echec_deja_existant() throws IOException {
        when(ftpClient.printWorkingDirectory()).thenReturn("/");
        when(ftpClient.changeWorkingDirectory(anyString())).thenReturn(true); // dossier existe

        FtpResponse<Void> response = ftpService.makeDirectory(ftpClient, "/existant");

        assertThat(response.succes()).isFalse();
        assertThat(response.message()).contains("existe déjà");
    }

    // ─────────────────────────────────────────────
    // disconnect
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("disconnect — se déconnecte proprement si connecté")
    void disconnect_si_connecte() throws IOException {
        when(ftpClient.isConnected()).thenReturn(true);

        ftpService.disconnect(ftpClient);

        verify(ftpClient).logout();
        verify(ftpClient).disconnect();
    }

    @Test
    @DisplayName("disconnect — ne fait rien si le client est null")
    void disconnect_si_null() {
        assertThatCode(() -> ftpService.disconnect(null))
                .doesNotThrowAnyException();
    }
}