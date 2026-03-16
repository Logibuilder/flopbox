package univ.sr2.flopbox.dto;


public record ServerRequest(
        String alias,
        String host,
        int port
) {
}