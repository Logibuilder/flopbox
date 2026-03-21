package univ.sr2.flopbox.dto;

import java.util.Map;

public record GlobalSearchRequest(
        String searchQuery,
        Map<String, ServerCredentials> credentials
) {
}
