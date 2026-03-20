package univ.sr2.flopbox.dto;

public record FtpResponse<T>(
        Boolean succes,
        String message,
        T data
) {
}
