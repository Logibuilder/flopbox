package univ.sr2.flopbox.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import univ.sr2.flopbox.dto.TypeToken;
import univ.sr2.flopbox.model.RefreshToken;
import univ.sr2.flopbox.model.User;
import univ.sr2.flopbox.repository.RefreshTokenRepository;
import univ.sr2.flopbox.repository.UserRepository;

import java.time.Instant;

@Service
public class RefreshTokenService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    @Transactional
    public RefreshToken createRefreshToken(String mail) {

        RefreshToken refreshToken = new RefreshToken();
        User user = userRepository.findByMail(mail)
                .orElseThrow(() -> new RuntimeException("L'utilisateur avec le mail : " + mail + " n'existe pas"));

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();
        refreshToken.setUser(user);

        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        String token = jwtService.generateToken(user, refreshTokenDurationMs, TypeToken.REFRESH);
        refreshToken.setToken(token);

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Le refresh token a expiré.");
        }
        return  token;
    }

    public void deletByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
