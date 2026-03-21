package univ.sr2.flopbox.dto;


/**
 * DTO utilisé pour cibler un serveur à supprimer de la base de données à partir de son nom d'hôte.
 */
public record DeleteServerRequest(
        String host
) {
}
