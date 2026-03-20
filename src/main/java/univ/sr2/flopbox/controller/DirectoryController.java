package univ.sr2.flopbox.controller;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.sr2.flopbox.dto.ApiResponse;
import univ.sr2.flopbox.dto.FtpItem;
import univ.sr2.flopbox.dto.FtpResponse;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.service.DirectoryService;
import univ.sr2.flopbox.service.ServerService;

import java.io.IOException;
import java.util.List;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@RestController
@RequestMapping("api/v1/servers/{host}/directories")
public class DirectoryController {

    @Autowired
    DirectoryService directoryService;

    @Autowired
    ServerService serverService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<FtpItem>>> listDirectory(
            @PathVariable("host") String host,
            @RequestParam("path") String path,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) {

        // 1. On passe par le service pour récupérer le serveur
        Server server = serverService.getServerByHost(host);
        log.debug("le server en question "+ String.valueOf(server));
        if (server == null) {
            // On utilise notre Record ApiResponse.error() pour garder la structure JSON propre
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "Serveur inconnu"));
        }

        FTPClient ftpClient = null;

        try {
            // Connexion via ServerService
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            // 3. Récupération des données sous forme de Record (FtpItem)
            List<FtpItem> items = directoryService.listDirectory(ftpClient, path);

            // Succès : On encapsule la liste dans le Record ApiResponse.success()
            return ResponseEntity.ok(ApiResponse.success(200, items, "Liste des fichier recupérée avec Succès"));

        } catch (IOException e) {
            // 5. Erreur FTP (ex: mauvais mot de passe)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, "Erreur d'accès FTP : " + e.getMessage()));
        } finally {
            // 6. Déconnexion automatique et propre
            serverService.disconnect(ftpClient);
        }
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<String>> makeDirectory(
            @PathVariable("host") String host,
            @RequestParam("path") String path,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) {

        Server server = serverService.getServerByHost(host);

        FTPClient ftpClient = null;

        try {

            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            FtpResponse<Void> ftpResponse = directoryService.makeDirectory(ftpClient, path);

            if (!ftpResponse.succes()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, "Échec de la création du dossier : " + ftpResponse.message()));
            }

            log.info("répertoire " + path + " créé avec succès");
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(201, path, ftpResponse.message()));

        } catch(Exception e) {
            log.error("Erreur interceptée lors de la création du dossier : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Erreur système : " + e.getMessage()));
        } finally {
            if (ftpClient != null) {
                serverService.disconnect(ftpClient);
                log.debug("Déconnexion du serveur FTP effectuée.");
            }
        }


    }

    @DeleteMapping()
    public ResponseEntity<ApiResponse<String>> deleteDirectory(
            @PathVariable String host,
            @RequestParam String path,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) throws IOException {

        Server server = serverService.getServerByHost(host);

        FTPClient ftpClient = null;

        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            FtpResponse<Void> ftpResponse = directoryService.delete(ftpClient, path);

            if (!ftpResponse.succes()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, ftpResponse.message()));
            }

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.error(200, "Suppréssion réussie"));
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
