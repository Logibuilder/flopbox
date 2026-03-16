package univ.sr2.flopbox.dto;

public record FtpItem(
        String name,
        String type, // "DIRECTORY" ou "FILE"
        long size,
        String lastModified
) {
}