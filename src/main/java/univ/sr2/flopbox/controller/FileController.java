package univ.sr2.flopbox.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
}