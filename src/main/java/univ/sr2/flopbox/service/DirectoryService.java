package univ.sr2.flopbox.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import univ.sr2.flopbox.dto.FtpItem;
import univ.sr2.flopbox.dto.FtpResponse;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class DirectoryService {

    @Autowired
    FTPService ftpService;

    public List<FtpItem> listDirectory(FTPClient ftpClient, String path) throws IOException {
        return ftpService.listDirectory(ftpClient, path);
    }


    public FtpResponse<Void> makeDirectory(FTPClient ftpClient, String path) {
        return ftpService.makeDirectory(ftpClient, path);
    }

    public FtpResponse<Void> delete(FTPClient ftpClient, String path) throws IOException {
        return ftpService.deleteDirectory(ftpClient, path);
    }
}
