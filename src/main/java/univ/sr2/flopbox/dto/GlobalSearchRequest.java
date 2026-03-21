package univ.sr2.flopbox.dto;

import java.util.Map;

/**
 * Requête envoyée par le client pour déclencher une recherche multi-serveurs.
 * Contient le mot-clé à chercher et la liste des serveurs sur lesquels chercher.
 */
public record GlobalSearchRequest(
        String searchQuery,
        Map<String, ServerCredentials> credentials
) {
}
