package univ.sr2.flopbox.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import univ.sr2.flopbox.dto.ApiResponse;
import univ.sr2.flopbox.dto.FtpResponse;
import univ.sr2.flopbox.dto.RenameRequest;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.service.FileService;
import univ.sr2.flopbox.service.ServerService;

import java.io.IOException;
import java.io.OutputStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/servers/{host}/files")
public class FileController {

    @Autowired
    FileService fileService;
    @Autowired
    ServerService serverService;



    @GetMapping()
    public void downloadFile(
            @PathVariable String host,
            @RequestParam String path,
            HttpServletResponse response,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) throws IOException {

        Server server = serverService.getServerByHost(host);

        FTPClient ftpClient = null;

        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            String fileName = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
            if (fileName.isEmpty()) fileName = "downloaded_file";
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            OutputStream outputStream = response.getOutputStream();

            fileService.downloadFile(ftpClient, path, outputStream);

            outputStream.flush();

        } catch (IOException e) {
            // En cas d'erreur technique (ex: fichier introuvable sur le FTP)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur FTP : " + e.getMessage());
        } finally {
            // 6. Toujours se déconnecter proprement
            if (ftpClient != null) {
                serverService.disconnect(ftpClient);
            }
        }
    }
    @PostMapping
    public void uploadFile(
            @PathVariable String host,
            @RequestParam String path,
            @RequestParam("file") MultipartFile file,
            HttpServletResponse response,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) throws IOException {

        Server server = serverService.getServerByHost(host);
        FTPClient ftpClient = null;

        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);
            fileService.uploadFile(ftpClient, path, file.getInputStream(), true);

            log.info("Succès de l'opération d'upload pour {}", host);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Upload réussi");

        } catch (Exception e) {
            log.error("Erreur interceptée dans le contrôleur : {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (ftpClient != null) {
                serverService.disconnect(ftpClient);
                log.debug("Déconnexion du serveur FTP effectuée.");
            }
        }

    }

    @PatchMapping()
    public ResponseEntity<ApiResponse<FtpResponse<Void>>>  rename(
            @PathVariable String host,
            @RequestBody RenameRequest renameRequest,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) throws IOException {


        Server server = serverService.getServerByHost(host);

        FTPClient ftpClient = null;

        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            FtpResponse<Void> ftpResponse = fileService.rename(ftpClient, renameRequest.oldName(), renameRequest.newName());

            if (!ftpResponse.succes()) {
                // Si c'est false, on renvoie une erreur HTTP (ex: 400 Bad Request) et on extrait le message du FTP
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "Échec du renommage FTP : " + ftpResponse.message()));
            }

            // Si tout est OK, on renvoie le succès
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(200, null, "Fichier renommé avec succès"));

        } catch (Exception e) {

            log.error("Erreur interceptée dans le contrôleur : {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "Erreur lors du renommage : " + e.getMessage()));

        } finally {
            if (ftpClient != null) {
                serverService.disconnect(ftpClient);
                log.debug("Déconnexion du serveur FTP effectuée.");
            }
        }
    }

    @DeleteMapping()
    public ResponseEntity<ApiResponse<FtpResponse<Void>>> deleteFile(
            @PathVariable String host,
            @RequestParam String path,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) throws IOException {

        Server server = serverService.getServerByHost(host);

        FTPClient ftpClient = null;

        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            FtpResponse<Void> ftpResponse = fileService.delete(ftpClient, path);

            if (!ftpResponse.succes()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, "Échec de suppression FTP : " + ftpResponse.message()));
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(200, "Suppréssion réussie : " + ftpResponse.message()));
        } catch (Exception e) {
            log.error("Erreur interceptée dans le contrôleur : {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "Erreur lors de la suppression : " + e.getMessage()));

        } finally {
            if (ftpClient != null) {
                serverService.disconnect(ftpClient);
                log.debug("Déconnexion du serveur FTP effectuée.");
            }
        }
    }
}
