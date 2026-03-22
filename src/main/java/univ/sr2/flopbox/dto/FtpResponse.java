package univ.sr2.flopbox.dto;


/**
 * Enveloppe interne utilisée par les services FTP pour faire remonter
 * le statut (succès/échec) et le message natif du serveur FTP vers les contrôleurs.
 *
 * @param <T> Le type de donnée éventuellement retournée par l'opération.
 */
public record FtpResponse<T>(
        Boolean succes,
        String message,
        int code,
        T data
) {
}
