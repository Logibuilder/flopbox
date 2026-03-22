package univ.sr2.flopbox.utils;

import org.springframework.http.HttpStatus;

public class FtpHttpStatusAdaptator {

    public static HttpStatus mapFtpCodeToHttpStatus(int ftpCode) {

        // NOUVEAU : Si le FTP dit "Créé" (257), on renvoie HTTP 201 (Created)
        if (ftpCode == 257) {
            return HttpStatus.CREATED;
        }

        // Pour les autres succès génériques (2xx et 3xx)
        if (ftpCode >= 200 && ftpCode < 400) {
            return HttpStatus.OK;
        }

        // Mapping des erreurs FTP classiques vers HTTP
        return switch (ftpCode) {
            case 431, 530, 532 -> HttpStatus.UNAUTHORIZED; // 401
            case 550 -> HttpStatus.NOT_FOUND;              // 404
            case 450, 451, 452 -> HttpStatus.CONFLICT;     // 409
            case 552 -> HttpStatus.INSUFFICIENT_STORAGE;   // 507
            case 553 -> HttpStatus.BAD_REQUEST;            // 400
            case 500, 501, 502, 503, 504 -> HttpStatus.BAD_REQUEST; // 400
            case 425, 426 -> HttpStatus.SERVICE_UNAVAILABLE; // 503
            default -> HttpStatus.INTERNAL_SERVER_ERROR;   // 500
        };
    }


    public static int mapFtpCodeToHttpCode(int ftpCode) {

        if (ftpCode == 257) {
            return 201;
        }

        if (ftpCode >= 200 && ftpCode < 400) {
            return 200;
        }

        return switch (ftpCode) {
            case 431, 530, 532 -> 401; // 401
            case 550 -> 404;
            case 450, 451, 452 -> 409;
            case 552 -> 507;
            case 553 -> 400;
            case 500, 501, 502, 503, 504 ->400;
            case 425, 426 -> 503;
            default -> 500;
        };
    }
}
