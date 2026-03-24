package univ.sr2.flopbox.service;


import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import univ.sr2.flopbox.dto.TypeToken;
import univ.sr2.flopbox.model.User;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private  SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }


    public String generateToken(User user, Long expiryTimeMs, TypeToken type) {

        return Jwts.builder()
                .subject(user.getMail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryTimeMs))
                .claim("token_type", type)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {

            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

            return true;

        } catch (JwtException e) {

            return false;
        }
    }

    public String getMailFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
