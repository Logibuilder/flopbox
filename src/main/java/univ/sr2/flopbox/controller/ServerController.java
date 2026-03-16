package univ.sr2.flopbox.controller;


import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.sr2.flopbox.dto.ApiResponse;
import univ.sr2.flopbox.dto.FtpItem;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.service.ServerService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/servers")
public class ServerController {

    @GetMapping
    public String hello() {
        return "Hello";
    }


    @Autowired
    private ServerService serverService;

    @GetMapping("/{id}/directory")
    public ResponseEntity<ApiResponse<List<FtpItem>>> listDirectory(
            @PathVariable("id") int id,
            @RequestParam("path") String path,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword) {

        // 1. On passe par le service pour récupérer le serveur
        Server server = serverService.getServerById(id);

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
            List<FtpItem> items = serverService.listDirectory(ftpClient, path);

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
}
