package univ.sr2.flopbox.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Tests unitaires - PasswordEncoderCoderConfig")
class PasswordEncoderConfigTest {

    private  final PasswordEncoderConfig config = new PasswordEncoderConfig();

    @Test
    @DisplayName("Vérifie la création du bean BCryptPasswordEncoder")
    void passwordEncoder_Creation() {
        PasswordEncoder encoder = config.PasswordEncoder();
        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);
    }


    @Test
    @DisplayName("Vérifie que le hachage et la correspondance fonctionnent")
    void passwordEncoder_Functionality() {
        PasswordEncoder encoder = config.PasswordEncoder();
        String rawPassword = "password123";
        String encoded = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded);
        assertTrue(encoder.matches(rawPassword, encoded));
    }
}