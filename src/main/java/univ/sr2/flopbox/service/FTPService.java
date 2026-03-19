package univ.sr2.flopbox.service;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Service;
import univ.sr2.flopbox.dto.FtpItem;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FTPService {

    /**
     * Se connecte à un serveur FTP distant
     */
    public FTPClient connect(String host, int port, String username, String password) throws IOException, IOException {
        FTPClient ftpClient = new FTPClient();

        // Connexion au serveur
        ftpClient.connect(host, port);

        // Vérification de la réponse
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new IOException("Le serveur FTP a refusé la connexion.");
        }

        // Authentification
        boolean success = ftpClient.login(username, password);
        if (!success) {
            ftpClient.disconnect();
            throw new IOException("Échec de l'authentification : identifiants FTP incorrects.");
        }

        return ftpClient;
    }

    public List<FtpItem> listDirectory(FTPClient ftpClient, String path) throws IOException {
        //en mode passive
        ftpClient.enterLocalPassiveMode();

        FTPFile[] files = ftpClient.listFiles(path);

        return Arrays.stream(files).map(file -> new FtpItem(
                file.getName(),
                file.isDirectory() ? "DIRECTORY" : "FILE",
                file.getSize(),
                file.getTimestamp() != null ? file.getTimestamp().getTime().toString() : "Inconnu"
        )).collect(Collectors.toList());
    }

    /**
     * Ferme la connexion proprement
     */
    public void disconnect(FTPClient ftpClient) {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                System.err.println("Erreur lors de la déconnexion FTP : " + e.getMessage());
            }
        }
    }

    public void downloadFIle(FTPClient ftpClient, String path, OutputStream outputStream) throws IOException {
        ftpClient.enterLocalPassiveMode();
        boolean success = ftpClient.retrieveFile(path, outputStream);

        if (!success) {
            throw new IOException("Erreur lors du téléchargement");
        }
    }

    public void uploadFile(FTPClient ftpClient, String path, InputStream inputStream, boolean replace) throws IOException {
        log.info("Début de la procédure d'upload pour le chemin : {}", path);

        log.debug("Passage en mode passif local...");
        ftpClient.enterLocalPassiveMode();

        // Vérifier si le fichier existe déjà
        log.debug("Vérification de l'existence du fichier : {}", path);
        String[] existingFiles = ftpClient.listNames(path);
        boolean exists = (existingFiles != null && existingFiles.length > 0);

        if (exists) {
            if (!replace) {
                log.warn("L'upload a échoué : le fichier existe déjà et le remplacement est désactivé.");
                throw new IOException("Le fichier existe déjà et le remplacement n'est pas autorisé : " + path);
            }
            log.info("Le fichier existe déjà. Le mode 'replace' est actif, écrasement en cours...");
        }

        try (inputStream) {
            log.debug("Exécution de ftpClient.storeFile()...");
            boolean success = ftpClient.storeFile(path, inputStream);

            if (!success) {
                String reply = ftpClient.getReplyString();
                log.error("Échec critique de l'upload. Réponse du serveur FTP : {}", reply);
                throw new IOException("Échec de l'upload (Réponse du serveur : " + reply + ")");
            }

            log.info("Upload terminé avec succès pour le fichier : {}", path);
        } catch (IOException e) {
            log.error("Erreur d'entrée/sortie pendant le transfert FTP : {}", e.getMessage(), e);
            throw e;
        }
    }

    public void deleteFile(FTPClient ftpClient, String path, OutputStream outputStream) {

    }

    public void downloadFile(FTPClient ftpClient, String path, OutputStream outputStream) throws IOException {

        ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

        ftpClient.enterLocalPassiveMode();

        boolean success = ftpClient.retrieveFile(path, outputStream);

        if (!success) {
            throw new IOException("Erreur lors du téléchargement du fichier : " + path);
        }
    }
}
