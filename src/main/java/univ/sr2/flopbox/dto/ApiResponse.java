package univ.sr2.flopbox.dto;

/**
 * Enveloppe générique pour uniformiser toutes les réponses de l'API REST.
 * Garantit que le client (Front-end) recevra toujours un JSON avec la même structure
 * (un code, un message, et éventuellement des données).
 *
 * @param <T> Le type de donnée encapsulée (Liste, Objet, String, etc.)
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data
) {

    // Méthode raccourci pour un succès (Code 200)
    public static <T> ApiResponse<T> success(int code, T data, String message) {
        return new ApiResponse<>(code, message, data);
    }

    // Méthode raccourci pour une erreur (Sans données)
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

