package univ.sr2.flopbox;



import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import univ.sr2.flopbox.dto.*;
import univ.sr2.flopbox.model.Server;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests unitaires — DTOs (Records)")
class DtoTest {

    // ─────────────────────────────────────────────
    // ApiResponse
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("ApiResponse.success — construit la réponse correctement")
    void apiResponse_success() {
        ApiResponse<String> response = ApiResponse.success(200, "données", "OK");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data()).isEqualTo("données");
        assertThat(response.message()).isEqualTo("OK");
    }

    @Test
    @DisplayName("ApiResponse.error — construit la réponse d'erreur sans data")
    void apiResponse_error() {
        ApiResponse<Object> response = ApiResponse.error(404, "Non trouvé");

        assertThat(response.code()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("Non trouvé");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("ApiResponse — code 201 pour une création")
    void apiResponse_created() {
        ApiResponse<String> response = ApiResponse.success(201, "id-123", "Créé");

        assertThat(response.code()).isEqualTo(201);
    }

    // ─────────────────────────────────────────────
    // ServerRequest
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("ServerRequest.toServer — convertit le DTO en entité JPA")
    void serverRequest_toServer() {
        ServerRequest request = new ServerRequest("MonAlias", "ftp.example.com", 2121);
        Server server = request.toServer();

        assertThat(server.getHost()).isEqualTo("ftp.example.com");
        assertThat(server.getAlias()).isEqualTo("MonAlias");
        assertThat(server.getPort()).isEqualTo(2121);
        assertThat(server.getId()).isEqualTo(0); // ID = 0 car géré par JPA
    }

    @Test
    @DisplayName("ServerRequest.toRequest — convertit l'entité en DTO")
    void serverRequest_toRequest() {
        Server server = new Server(42, "ftp.example.com", "MonAlias", 21);
        ServerRequest request = ServerRequest.toRequest(server);

        assertThat(request.host()).isEqualTo("ftp.example.com");
        assertThat(request.alias()).isEqualTo("MonAlias");
        assertThat(request.port()).isEqualTo(21);
    }

    @Test
    @DisplayName("ServerRequest — toServer → toRequest est idempotent")
    void serverRequest_roundtrip() {
        ServerRequest original = new ServerRequest("Alias", "ftp.test.com", 21);
        ServerRequest roundtrip = ServerRequest.toRequest(original.toServer());

        assertThat(roundtrip.host()).isEqualTo(original.host());
        assertThat(roundtrip.alias()).isEqualTo(original.alias());
        assertThat(roundtrip.port()).isEqualTo(original.port());
    }

    // ─────────────────────────────────────────────
    // FtpResponse
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("FtpResponse — représente un succès FTP")
    void ftpResponse_succes() {
        FtpResponse<Void> response = new FtpResponse<>(true, "OK", null);

        assertThat(response.succes()).isTrue();
        assertThat(response.message()).isEqualTo("OK");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("FtpResponse — représente un échec FTP avec message natif")
    void ftpResponse_echec() {
        FtpResponse<Void> response = new FtpResponse<>(false, "550 Permission denied", null);

        assertThat(response.succes()).isFalse();
        assertThat(response.message()).contains("550");
    }

    // ─────────────────────────────────────────────
    // FtpItem
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("FtpItem — un fichier a le bon type FILE")
    void ftpItem_type_file() {
        FtpItem item = new FtpItem("/pub/readme.txt", "readme.txt", Type.FILE, 1024, "2025-01-01");

        assertThat(item.type()).isEqualTo(Type.FILE);
        assertThat(item.name()).isEqualTo("readme.txt");
        assertThat(item.size()).isEqualTo(1024L);
    }

    @Test
    @DisplayName("FtpItem — un répertoire a le bon type DIRECTORY")
    void ftpItem_type_directory() {
        FtpItem item = new FtpItem("/pub/docs", "docs", Type.DIRECTORY, 0, "2025-01-01");

        assertThat(item.type()).isEqualTo(Type.DIRECTORY);
        assertThat(item.size()).isEqualTo(0L);
    }

    // ─────────────────────────────────────────────
    // SearchResponse
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("SearchResponse — associe correctement un FtpItem à un ServerRequest")
    void searchResponse_association() {
        FtpItem item = new FtpItem("/readme.txt", "readme.txt", Type.FILE, 100, "now");
        ServerRequest serverReq = new ServerRequest("A", "ftp.a.com", 21);
        SearchResponse response = new SearchResponse(item, serverReq);

        assertThat(response.ftpItem()).isSameAs(item);
        assertThat(response.serverRequest()).isSameAs(serverReq);
        assertThat(response.serverRequest().host()).isEqualTo("ftp.a.com");
    }

    // ─────────────────────────────────────────────
    // RenameRequest
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("RenameRequest — stocke correctement les deux chemins")
    void renameRequest() {
        RenameRequest req = new RenameRequest("/ancien.txt", "/nouveau.txt");

        assertThat(req.oldName()).isEqualTo("/ancien.txt");
        assertThat(req.newName()).isEqualTo("/nouveau.txt");
    }

    // ─────────────────────────────────────────────
    // DeleteServerRequest
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("DeleteServerRequest — stocke correctement l'hôte")
    void deleteServerRequest() {
        DeleteServerRequest req = new DeleteServerRequest("ftp.example.com");

        assertThat(req.host()).isEqualTo("ftp.example.com");
    }
}