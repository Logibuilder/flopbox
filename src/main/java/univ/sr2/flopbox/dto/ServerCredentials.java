package univ.sr2.flopbox.dto;

public record ServerCredentials(
        String host,
        String username,
        String password
) {
}
