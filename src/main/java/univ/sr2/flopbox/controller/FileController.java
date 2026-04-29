package univ.sr2.flopbox.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import univ.sr2.flopbox.dto.ApiResponse;
import univ.sr2.flopbox.dto.FtpResponse;
import univ.sr2.flopbox.dto.RenameRequest;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.service.FileService;
import univ.sr2.flopbox.service.FtpInputStream;
import univ.sr2.flopbox.service.ServerService;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import univ.sr2.flopbox.utils.FtpHttpStatusAdaptator;

@Slf4j
@RestController
@Tag(name = "Fichiers", description = "API de manipulation des fichiers sur un serveur FTP distant (Upload, Download, Renommer, Supprimer)")
@RequestMapping("api/v1/servers/{host}/files")
public class FileController {

    @Autowired
    FileService fileService;
    @Autowired
    ServerService serverService;


    @Operation(summary = "Télécharger un fichier", description = "Récupère un fichier depuis le serveur FTP et le télécharge (Download).")
    @GetMapping()
    public ResponseEntity<?> downloadFile(
            @PathVariable String host,
            @RequestParam String path,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) throws IOException {

        Server server = serverService.getServerByHost(host);

        FTPClient ftpClient = null;
        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            String fileName = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
            if (fileName.isEmpty()) fileName = "downloaded_file";

            InputStream inputStream = fileService.downloadFile(ftpClient, path);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachement", fileName);

            return  ResponseEntity.ok().headers(headers).body(new InputStreamResource(new FtpInputStream(ftpClient, inputStream)));


        } catch (IOException e) {
            if (ftpClient != null) {
                serverService.disconnect(ftpClient);
                log.debug("Déconnexion de secours effectuée suite à une erreur de téléchargement.");
            }
            log.error("Erreur lors du téléchargement depuis {} : {}", host, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }


    @Operation(summary = "Envoyer un fichier", description = "Envoie un fichier local vers le serveur FTP distant (Upload). Écrase le fichier s'il existe déjà.")
    @PostMapping
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @PathVariable String host,
            @RequestParam String path,
            @RequestBody byte[] file,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) {

        Server server = serverService.getServerByHost(host);
        FTPClient ftpClient = null;

        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            InputStream inputStream = new ByteArrayInputStream(file);
            FtpResponse<Void> ftpResponse = fileService.uploadFile(ftpClient, path, inputStream, true);

            HttpStatus httpStatus = FtpHttpStatusAdaptator.mapFtpCodeToHttpStatus(ftpResponse.code());

            if (!ftpResponse.succes()) {
                return ResponseEntity.status(httpStatus)
                        .body(ApiResponse.error(httpStatus.value(), "Échec de l'upload FTP : " + ftpResponse.message()));
            }
            log.info("Succès de l'opération d'upload pour {}", host);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(201, path, "Upload réussi avec succès"));

        } catch (Exception e) {
            log.error("Erreur interceptée dans le contrôleur lors de l'upload : {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Erreur lors de l'upload : " + e.getMessage()));

        } finally {
            if (ftpClient != null) {
                serverService.disconnect(ftpClient);
                log.debug("Déconnexion du serveur FTP effectuée.");
            }
        }
    }


    @Operation(summary = "Renommer un fichier", description = "Modifie le nom d'un fichier existant sur le serveur FTP.")
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


            HttpStatus httpStatus = FtpHttpStatusAdaptator.mapFtpCodeToHttpStatus(ftpResponse.code());

            if (!ftpResponse.succes()) {
                return ResponseEntity.status(httpStatus)
                        .body(ApiResponse.error(httpStatus.value(), "Échec du renommage FTP : " + ftpResponse.message()));
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
    @Operation(summary = "Supprimer un fichier", description = "Supprime définitivement un fichier sur le serveur FTP.")
    @DeleteMapping()
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String host,
            @RequestParam String path,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) throws IOException {

        Server server = serverService.getServerByHost(host);

        FTPClient ftpClient = null;

        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            FtpResponse<Void> ftpResponse = fileService.delete(ftpClient, path);

            HttpStatus httpStatus = FtpHttpStatusAdaptator.mapFtpCodeToHttpStatus(ftpResponse.code());

            if (!ftpResponse.succes()) {
                return ResponseEntity.status(httpStatus)
                        .body(ApiResponse.error(httpStatus.value(), "Échec de suppression FTP : " + ftpResponse.message()));
            }

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(200,null , "Suppréssion réussie : " + ftpResponse.message()));

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
