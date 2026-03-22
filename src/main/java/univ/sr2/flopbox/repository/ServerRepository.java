package univ.sr2.flopbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import univ.sr2.flopbox.model.Server;

import java.util.Optional;


@Repository
public interface ServerRepository extends JpaRepository<Server, Integer> {
    Optional<Server> findByHost(String host);
}
