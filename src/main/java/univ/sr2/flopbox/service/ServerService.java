package univ.sr2.flopbox.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import univ.sr2.flopbox.dto.*;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.repository.ServerRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Service
public class ServerService {

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    FTPService ftpService;

    public FTPClient connect(Server server, String username, String password ) throws IOException {

        return ftpService.connect(server.getHost(), server.getPort(), username, password);
    }

    public FTPClient toPassiveMode(FTPClient ftpClient) {
        ftpClient.enterLocalPassiveMode();
        return ftpClient;
    }

    public void disconnect(FTPClient ftpClient) {
        ftpService.disconnect(ftpClient);
    }

    /**
     * méthode pour récupérer un serveur par son ID
     */
    public Server getServerById(int id) {
        return serverRepository.findById(id).orElse(null);
    }



    public  Server addServer(ServerRequest serverRequest) {

        if (serverRepository.findByHost(serverRequest.host()).isPresent()) {
            throw new RuntimeException("Un serveur avec cet hôte existe déjà.");
        }
        return serverRepository.save(serverRequest.toServer());
    }

    public List<Server> getServer() {

        return  serverRepository.findAll();
    }

    public Server deleteServer(DeleteServerRequest deleteServerRequest) {
        Server server = serverRepository.findByHost(deleteServerRequest.host())
                .orElseThrow(() -> new RuntimeException("Serveur non trouvé avec l'hôte : " + deleteServerRequest.host()));

        serverRepository.delete(server);

        //Renvoyer l'objet pour que le contrôleur puisse confirmer ce qui a été supprimé
        return server;
    }

    public Server getServerByHost(String host) {
        return serverRepository.findByHost(host)
                .orElseThrow(() -> new RuntimeException("Serveur non trouvé avec l'hôte : " + host));
    }

    public Server updateServer(String currentHost, ServerRequest updateRequest) {
        Server existingServer = serverRepository.findByHost(currentHost)
                .orElseThrow(() -> new RuntimeException("Serveur non trouvé avec l'hôte : " + currentHost));

        // vérifier si le NOUVEL hôte existe déjà (pour éviter les doublons si on change le host)
        if (!currentHost.equals(updateRequest.host()) && serverRepository.findByHost(updateRequest.host()).isPresent()) {
            throw new RuntimeException("Un serveur avec ce nouvel hôte existe déjà.");
        }

        // On met à jour les champs
        existingServer.setAlias(updateRequest.alias());
        existingServer.setHost(updateRequest.host());
        existingServer.setPort(updateRequest.port());

        // On sauvegarde les modifications dans la base de données
        return serverRepository.save(existingServer);
    }

    public List<FtpItem> searchFile(FTPClient ftpClient, String searchQuery) throws IOException {
        return  ftpService.seachFile(ftpClient, searchQuery);
    }

    public List<SearchResponse> searchGlobal(String seaachQuery, Map<String, ServerCredentials> credentials) throws IOException {

        List<SearchResponse> resGlobal = new ArrayList<SearchResponse>();

        if (credentials == null || credentials.isEmpty()) {
            return resGlobal;
        }


        for (ServerCredentials creds :  credentials.values()) {

            FTPClient ftpClient = null;

            try {

                Server server = serverRepository.findByHost(creds.host())
                        .orElseThrow(() -> new RuntimeException("Le serveur " + creds.host() + " n'est pas enregistré dans Flopbox."));

                ftpClient = this.connect(server, creds.username(), creds.password());

                List<FtpItem> res = searchFile(ftpClient, seaachQuery);

                resGlobal.addAll(
                        res.stream()
                                .map(
                                        ftpItem -> new SearchResponse(
                                                ftpItem
                                                , ServerRequest.toRequest(server)
                                        ))
                                .toList()
                );

            } catch (Exception e) {
            // Si CE serveur précis plante (mauvais mdp, hors-ligne...), on l'ignore et on passe au suivant
                log.warn("Impossible de charcher dans le serveur {} : {}", creds.host(), e.getMessage());
            } finally {
                // déconnecter
                if (ftpClient != null) {
                    this.disconnect(ftpClient);
                }
            }
        }

        return resGlobal;
    }
}
