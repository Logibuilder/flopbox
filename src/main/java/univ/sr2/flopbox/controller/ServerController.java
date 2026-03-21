package univ.sr2.flopbox.controller;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.sr2.flopbox.dto.*;
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

    @PutMapping("/{host}")
    public ResponseEntity<ApiResponse<ServerRequest>> updateServer(
            @PathVariable String host,
            @RequestBody ServerRequest updateRequest) {

        try {
            // Appel au service pour faire la mise à jour
            Server updatedServer = serverService.updateServer(host, updateRequest);
            ServerRequest responseDto = ServerRequest.toRequest(updatedServer);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(200, responseDto, "Serveur mis à jour avec succès"));
        } catch (RuntimeException e) {
            // Si le serveur n'existe pas, on renvoie une 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    @GetMapping("/{host}/search")
    public ResponseEntity<ApiResponse<List<SearchResponse>>>  searchFile(
            @PathVariable("host") String host,
            @RequestParam String searchQuery,
            @RequestHeader(value = "X-FTP-Username", defaultValue = "anonymous") String ftpUser,
            @RequestHeader(value = "X-FTP-Password", defaultValue = "") String ftpPassword){//} throws IOException {

        Server server = serverService.getServerByHost(host);

        FTPClient ftpClient = null;

        try {
            ftpClient = serverService.connect(server, ftpUser, ftpPassword);

            List<SearchResponse> searchResponses =  serverService.searchFile(ftpClient, searchQuery).stream().map(file -> new SearchResponse(file, ServerRequest.toRequest(server))).toList();

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(200,searchResponses, "recherche effectué avec succès"));
        } catch (Exception e){
            log.error("Erreur lors de la recherche sur {} : {}", host, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Impossible de joindre le serveur : " + e.getMessage()));
        } finally {
            if (ftpClient != null) {
                serverService.disconnect(ftpClient);
            }
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SearchResponse>>> searchGlobal(
            @RequestBody GlobalSearchRequest globalSearchRequest) {
        try {

            List<SearchResponse> allRes = serverService.searchGlobal(globalSearchRequest.searchQuery(), globalSearchRequest.credentials());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(200, allRes, "Recherche globale effectuée avec succès"));
        } catch(Exception e) {
            log.error("Erreur critique lors de la recherche globale : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Erreur serveur : " + e.getMessage()));
        }
    }
 }
