package univ.sr2.flopbox.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import univ.sr2.flopbox.dto.FtpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@NoArgsConstructor
@AllArgsConstructor
@Service
public class  FileService {

    @Autowired
    FTPService ftpService;

    public void downloadFile(FTPClient ftpClient, String path, OutputStream outputStream) throws IOException {
        ftpService.downloadFile(ftpClient, path, outputStream);
    }

    public void uploadFile(FTPClient ftpClient, String path, InputStream inputStream, boolean replace) throws IOException {
        ftpService.uploadFile(ftpClient, path, inputStream, replace);
    }

    public FtpResponse<Void> delete(FTPClient ftpClient, String path) throws IOException {
        return ftpService.deleteFile(ftpClient, path);
    }

    public FtpResponse<Void> rename(FTPClient ftpClient, String oldName, String newName) throws IOException {
        return ftpService.rename(ftpClient, oldName, newName);
    }

}
