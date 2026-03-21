package univ.sr2.flopbox.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class FtpInputStream extends FilterInputStream {

    private final FTPClient ftpClient;

    public FtpInputStream(FTPClient ftpClient, InputStream in) {
        super(in);
        this.ftpClient = ftpClient;
    }

    @Override
    public void close() throws IOException {
        try {
            // D'abord on ferme le flux de base
            super.close();
        } finally {
            // ENSUITE, on prévient le serveur FTP qu'on a fini
            try {
                if (ftpClient != null && ftpClient.isConnected()) {
                    boolean completed = ftpClient.completePendingCommand();
                    if (!completed) {
                        log.warn("Impossible de compléter la commande FTP après le téléchargement.");
                    }
                    // On ferme aussi la connexion FTP ici puisque le contrôleur a déjà renvoyé la réponse !
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                log.error("Erreur lors de la déconnexion FTP après téléchargement : {}", e.getMessage());
            }
        }
    }
}