package univ.sr2.flopbox.dto;


public record FtpItem(
        String path,
        String name,
        Type type, // "DIRECTORY" ou "FILE"
        long size,
        String lastModified
) {

}