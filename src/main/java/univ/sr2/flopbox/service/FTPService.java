package univ.sr2.flopbox.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Service;
import univ.sr2.flopbox.dto.FtpItem;
import lombok.extern.slf4j.Slf4j;
import univ.sr2.flopbox.dto.FtpResponse;
import univ.sr2.flopbox.dto.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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

    /**
     * Liste le contenu d'un répertoire donné sur le serveur FTP distant.
     * Ignore intelligemment les répertoires virtuels de navigation (ex: "." et "..") si nécessaire.
     *
     * @param ftpClient Le client FTP connecté.
     * @param path Le chemin absolu ou relatif du répertoire à lister.
     * @return Une liste d'objets FtpItem représentant les fichiers et dossiers trouvés.
     * @throws IOException En cas d'erreur de communication avec le serveur FTP.
     */
    public List<FtpItem> listDirectory(FTPClient ftpClient, String path) throws IOException {
        ftpClient.enterLocalPassiveMode();

        FTPFile[] files = ftpClient.listFiles(path);

        // Sécurité anti-crash au cas où le serveur refuse de lister le dossier
        if (files == null) {
            log.warn("Le serveur n'a renvoyé aucun fichier pour le chemin : '{}'", path);
            return new ArrayList<>();
        }

        log.info("Dossier '{}' listé : {} éléments trouvés.", path, files.length);

        return Arrays.stream(files).map(file -> {
            String cleanPath = path.endsWith("/") ? path + file.getName() : path + "/" + file.getName();

            return new FtpItem(
                    cleanPath,
                    file.getName(),
                    file.isDirectory() ? Type.DIRECTORY : Type.FILE,
                    file.getSize(),
                    file.getTimestamp() != null ? file.getTimestamp().getTime().toString() : "Inconnu"
            );
        }).collect(Collectors.toList());
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
    /**
     * Transfère un fichier local vers le serveur FTP distant (Upload) en mode binaire passif.
     * Vérifie au préalable si le fichier existe déjà pour éviter les écrasements accidentels,
     * sauf si le paramètre 'replace' l'autorise.
     *
     * @param ftpClient Le client FTP connecté et authentifié.
     * @param path Le chemin cible complet (incluant le nom du fichier) sur le serveur FTP.
     * @param inputStream Le flux de données du fichier à envoyer.
     * @param replace Si true, écrase le fichier cible s'il existe déjà. Si false, annule l'opération.
     * @return Un objet FtpResponse contenant le statut, le message et le code de réponse natif du serveur FTP.
     */
    public FtpResponse<Void> uploadFile(FTPClient ftpClient, String path, InputStream inputStream, boolean replace) {
        log.info("Début de la procédure d'upload pour le chemin : {}", path);
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            log.debug("Passage en mode passif local...");
            ftpClient.enterLocalPassiveMode();

            // Vérifier si le fichier existe déjà
            log.debug("Vérification de l'existence du fichier : {}", path);
            String[] existingFiles = ftpClient.listNames(path);
            boolean exists = (existingFiles != null && existingFiles.length > 0);

            if (exists && !replace) {
                log.warn("L'upload a échoué : le fichier existe déjà.");
                // Si on refuse l'écrasement, on simule une erreur 550 (non autorisé/introuvable) ou 450
                return new FtpResponse<>(false, "Le fichier existe déjà et le remplacement n'est pas autorisé", 450, null);
            }

            try (inputStream) {
                log.debug("Exécution de ftpClient.storeFile()...");
                boolean success = ftpClient.storeFile(path, inputStream);
                int ftpCode = ftpClient.getReplyCode(); // On récupère le code (souvent 226 Transfer Complete)

                if (!success) {
                    String reply = ftpClient.getReplyString();
                    log.error("Échec critique de l'upload. Réponse du serveur FTP : {}", reply);
                    return new FtpResponse<>(false, "Échec de l'upload : " + reply, ftpCode, null);
                }

                log.info("Upload terminé avec succès pour le fichier : {}", path);
                return new FtpResponse<>(true, "Upload terminé avec succès", ftpCode, null);
            }
        } catch (IOException e) {
            log.error("Erreur d'entrée/sortie pendant le transfert FTP : {}", e.getMessage(), e);
            // Code 500 générique en cas de crash réseau
            return new FtpResponse<>(false, "Erreur réseau pendant l'upload : " + e.getMessage(), 500, null);
        }
    }

    public InputStream downloadFile(FTPClient ftpClient, String path) throws IOException {

        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

        ftpClient.enterLocalPassiveMode();

        InputStream inputStream =   ftpClient.retrieveFileStream(path);
        if (inputStream == null) {
            String replyCode = String.valueOf(ftpClient.getReplyCode());
            String replyMessage = ftpClient.getReplyString().trim();
            throw new IOException("Erreur FTP " + replyCode + " : " + replyMessage);
        }

        return inputStream;
    }

    public FtpResponse<Void> rename(FTPClient ftpClient, String oldName, String newName) throws IOException {

        try {
            boolean success = ftpClient.rename(oldName, newName);

            int ftpCode = ftpClient.getReplyCode();
            if (!success) {
                return new FtpResponse<>(
                        false,
                        ftpClient.getReplyString(),
                        ftpCode,
                        null);
            }

            return new FtpResponse<>(
                    true,
                    "Fichier renommé avec succès",
                    ftpCode,
                    null);
        } catch (IOException e) {
            log.error("Erreur IO pendant l'opération : {}", e.getMessage(), e);
            return new FtpResponse<>(
                    false,
                    "Erreur IO : " + e.getMessage(), // Concaténation standard en Java
                    ftpClient.getReplyCode(),
                    null);
        }
    }

    public FtpResponse<Void> deleteFile(FTPClient ftpClient, String path) throws IOException {
        try {
            boolean success = ftpClient.deleteFile(path);

            if (success) {
                return new FtpResponse<>(true, path + " supprimé avec succès", ftpClient.getReplyCode(), null);
            }


            return new FtpResponse<>(false, ftpClient.getReplyString(),ftpClient.getReplyCode(), null);

        } catch (IOException e) {

            log.error("Erreur IO pendant la suppression : {}", e.getMessage(), e);

            return new FtpResponse<>(false, "Erreur IO suppression : " + e.getMessage(),ftpClient.getReplyCode(), null);
        }
    }

    public FtpResponse<Void> deleteDirectory(FTPClient ftpClient, String path) throws IOException {
        try {
            boolean success = ftpClient.removeDirectory(path);

            if (success) {
                return new FtpResponse<>(true, path + " supprimé avec succès",ftpClient.getReplyCode(), null);
            }


            return new FtpResponse<>(false, ftpClient.getReplyString(),ftpClient.getReplyCode(), null);

        } catch (IOException e) {

            log.error("Erreur IO pendant la suppression : {}", e.getMessage());

            return new FtpResponse<>(false, "Erreur IO suppression : " + e.getMessage(),ftpClient.getReplyCode(), null);
        }
    }



    public FtpResponse<Void> makeDirectory(FTPClient ftpClient, String path) {

        try {

            String currentWorkingDirectory = ftpClient.printWorkingDirectory();

            boolean alreadyExist = ftpClient.changeWorkingDirectory(path);

            if (alreadyExist) {

                ftpClient.changeWorkingDirectory(currentWorkingDirectory);

                return new FtpResponse<>(
                        false,
                        "Un répertoire du même nom(" + path+ ") existe déjà",
                        ftpClient.getReplyCode(),
                        null
                );
            }

            boolean success = ftpClient.makeDirectory(path);

            if (!success) {
                return new FtpResponse<>(
                        false,
                        ftpClient.getReplyString(),
                        ftpClient.getReplyCode(),
                        null
                );
            }

            return  new FtpResponse<>(
                    true,
                    "Répertoire cré avec succès",
                    ftpClient.getReplyCode(),
                    null);
        } catch (IOException e) {
            return  new FtpResponse<>(
                    false,
                    "Erreur IO création répertoire : " + e.getMessage(),
                    ftpClient.getReplyCode(),
                    null
            );
        }
    }
    public List<FtpItem> seachFile(FTPClient ftpClient, String searchQuery) throws IOException {
        List<FtpItem> resultats = new ArrayList<>();

        seachFile(ftpClient, "/", searchQuery, resultats, 0);
        return resultats;
    }

    /**
     * Parcourt l'arborescence du serveur FTP de manière récursive pour trouver des fichiers.
     * S'arrête automatiquement si la profondeur maximale est atteinte ou si suffisamment de
     * fichiers ont été trouvés pour éviter de surcharger la mémoire.
     *
     * @param ftpClient Le client FTP connecté au serveur.
     * @param currentPath Le chemin du répertoire actuellement inspecté.
     * @param searchQuery Le mot-clé à rechercher dans le nom des fichiers.
     * @param res La liste accumulant les résultats trouvés (passée par référence).
     * @param currentDepth La profondeur actuelle dans l'arborescence (0 pour la racine).
     * @throws IOException Si la lecture d'un dossier échoue (ex: droits insuffisants).
     */
    public void seachFile(FTPClient ftpClient,String currentPath, String searchQuery, List<FtpItem> res, int currentDepth) throws IOException {


        if (res.size() >= 5 || currentDepth >= 3) return;
        try {
            for (FtpItem f : this.listDirectory(ftpClient, currentPath)) {

                if (f.name().equals(".") || f.name().equals("..")) {
                    continue;
                }

                if (f.name().toLowerCase().contains(searchQuery.toLowerCase())) {
                    res.add(f);
                    if (res.size() >= 5) return;
                }

                if (f.type() == Type.DIRECTORY) {
                    seachFile(ftpClient, f.path(), searchQuery, res, currentDepth + 1);
                }
            }
        } catch (IOException e) {
            log.warn("Impossible de lire le dossier {} : {}", currentPath, e.getMessage());
        }
    }
}
