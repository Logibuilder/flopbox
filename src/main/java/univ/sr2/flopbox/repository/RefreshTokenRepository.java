package univ.sr2.flopbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import univ.sr2.flopbox.model.RefreshToken;
import univ.sr2.flopbox.model.User;

import java.util.Optional;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken>  findByToken(String token);

    @Transactional
    @Modifying
    void deleteByUser(User user);
}
