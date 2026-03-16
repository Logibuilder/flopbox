package univ.sr2.flopbox.controller;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.sr2.flopbox.dto.ApiResponse;
import univ.sr2.flopbox.dto.DeleteServerRequest;
import univ.sr2.flopbox.dto.FtpItem;
import univ.sr2.flopbox.dto.ServerRequest;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.service.ServerService;

import java.io.IOException;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("api/v1/servers")
public class ServerController {


    @Autowired
    private ServerService serverService;

    @GetMapping("/{host}/directory")
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

    @PostMapping()
    public ResponseEntity<ApiResponse<ServerRequest>> addServer(@RequestBody ServerRequest serverRequest) {

        try {
            Server savedServer = serverService.addServer(serverRequest);
            ServerRequest responseDto = ServerRequest.toRequest(savedServer);
            return ResponseEntity.status(HttpStatus.CREATED
            ).body(ApiResponse.success(201, responseDto, "Serveur créé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<ServerRequest>>> getServeur() {
        List<ServerRequest> responseDto =  serverService.getServer().stream().map(ServerRequest::toRequest).toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(200, responseDto, "Liste des serveurs recupérée avec succés"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<ServerRequest>> deleteServer(@RequestBody DeleteServerRequest deleteServerRequest) {

        try {
            Server deletedServer = serverService.deleteServer(deleteServerRequest);
            ServerRequest responseDto = ServerRequest.toRequest(deletedServer);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(200,responseDto, "Serveur supprimé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }
 }
