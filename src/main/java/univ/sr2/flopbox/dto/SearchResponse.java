package univ.sr2.flopbox.dto;

/**
 * Représente un résultat de recherche globale.
 * Associe le fichier ou dossier trouvé (FtpItem) avec les informations
 * du serveur (ServerRequest) sur lequel il réside.
 */
public record SearchResponse(
        FtpItem ftpItem,
        ServerRequest serverRequest
) {
}
