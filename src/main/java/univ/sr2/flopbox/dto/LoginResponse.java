package univ.sr2.flopbox.dto;

import univ.sr2.flopbox.model.User;

public record LoginResponse(
        String mail,
        String nom,
        String accessToken,
        String refreshToken
) {

    public static LoginResponse toResponse(User user, String accessToken, String refreshToken) {
        return new LoginResponse(user.getMail(), user.getName(),accessToken, refreshToken);
    }
}
