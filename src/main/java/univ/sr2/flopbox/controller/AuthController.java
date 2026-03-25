package univ.sr2.flopbox.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.sr2.flopbox.dto.ApiResponse;
import univ.sr2.flopbox.dto.LoginRequest;
import univ.sr2.flopbox.dto.LoginResponse;
import univ.sr2.flopbox.dto.UserRequest;
import univ.sr2.flopbox.model.User;
import univ.sr2.flopbox.service.UserService;

import javax.naming.spi.ResolveResult;

@Slf4j
@RestController
@Tag(description = "Controller pour la création, la connexion et la déconnexion des utilisateurs", name = "Authenfication et autorisation")
@RequestMapping("api/v1/auth")
public class AuthController {


    @Autowired
    UserService userService;


    @Operation(summary = "S'enregistrer", description = "Crée un nouveau compte utilisateur.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRequest>> register(
            @RequestBody UserRequest userRequest) {

        try {
            UserRequest userRequestSaved = userService.register(userRequest);

            return  ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(201, userRequestSaved, "Utilisateur créé avec succès"));
        } catch (RuntimeException e) {
            log.error("");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, "Echec inscription : " + e.getMessage()));
        }
    }

    @Operation(summary = "Se connecter", description = "Se connecter et avoir un accessToken et un refreshToken.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest loginRequest
            ) {
        try {
            LoginResponse loginResponse = userService.login(loginRequest);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(200, loginResponse, "Connexion réussie"));
        } catch (RuntimeException e) {
            log.error("Erreur de connexion pour {}: {}", loginRequest.mail(), e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(401, "Échec de l'authentification : " + e.getMessage()));
        }
    }
    @Operation(summary = "Se déconnecter ", description = "Se déconnecter en supprimant son refreshToken")
    @GetMapping("/logout")
    public ResponseEntity<ApiResponse<UserRequest>> logout() {
        try {
            UserRequest userRequestDisconnected =  userService.logout();
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(200, userRequestDisconnected, "Déconnection réussie"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(403, e.getMessage()));
        }
    }

}
