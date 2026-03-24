package univ.sr2.flopbox.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import univ.sr2.flopbox.dto.LoginRequest;
import univ.sr2.flopbox.dto.LoginResponse;
import univ.sr2.flopbox.dto.TypeToken;
import univ.sr2.flopbox.dto.UserRequest;
import univ.sr2.flopbox.model.RefreshToken;
import univ.sr2.flopbox.model.User;
import univ.sr2.flopbox.repository.UserRepository;

import java.util.Optional;


@Transactional
@Service
public class UserService {


    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Value("${jwt.access.expiration}")
    private Long accessTokenDurationMs;

    public UserRequest register(UserRequest userRequest) {

        if (userRepository.findByMail(userRequest.mail()).isPresent()) throw new RuntimeException("L'utilisateur " + userRequest.mail() + " existe déjà");

        try {
            User user = userRequest.toUser();
            user.setPassword(passwordEncoder.encode(userRequest.password()));
            userRepository.save(user);
            return userRequest;
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Erreur d'intégrité : l'email est peut-être déjà utilisé.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur technique lors de la sauvegarde : " + e.getMessage());
        }
    }

    public Optional<User> getUserByMail(String mail) {
        return userRepository.findByMail(mail);
    }

    public LoginResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByMail(loginRequest.mail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + loginRequest.mail()));

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }


        // Générer l'Access Token
        String accessToken = jwtService.generateToken(user, accessTokenDurationMs, TypeToken.ACCESS);

        // Générer le Refresh Token (JWT long stocké en base)
        RefreshToken refreshToken =  refreshTokenService.createRefreshToken(user.getMail());

        // Retourner la réponse (Le Refresh Token est en base, on renvoie l'Access Token au client)
        return new LoginResponse(user.getMail(), user.getName(), accessToken, refreshToken.getToken());

    }

    public UserRequest logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("Opération refusée : vous n'êtes pas connecté.");
        }

        String mail = authentication.getName();

        User user = userRepository.findByMail(mail).orElseThrow(() -> new RuntimeException("Utilisateur introuvable pour la déconnexion."));
        UserRequest userRequest = UserRequest.toUserRequest(user);
        try {
            refreshTokenService.deletByUser(user);
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            throw new RuntimeException("Erreur technique lors de la déconnexion : " + e.getMessage());
        }
        return userRequest;
    }

}
